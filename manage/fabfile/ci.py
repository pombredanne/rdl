from fabric.api import *
from fabric.contrib.files import exists
from server import tar_and_ftp_push, clean_path, create_path
from util import get_value_from_password_store, PASSWORD_FILE_FTP_USERNAME_PARAM_NAME, \
    PASSWORD_FILE_FTP_PASSWORD_PARAM_NAME, msg_sleep
from server import ftp_fetch_and_untar

BACKUP_FILE_LOCATION = "/var/lib/jenkins/thinbackup"

@task
@roles('ci')
def bootstrap():
    server.bootstrap()


@task
@roles('ci')
def install():
    os_update()
    install_prerequisites()
    install_java()
    install_git()
    install_maven()
    install_fabric()
    install_jenkins()


def install_prerequisites():
    os_install("curl")
    os_install("unzip")
    os_install("build-essential python-dev python-pkg-resources python-setuptools")


def install_git():
    os_install("git")


def install_java():
    os_install("openjdk-7-jre-headless openjdk-7-jdk")


def install_fabric():
    os_install("python-lxml")
    sudo("sudo easy_install pip")
    sudo("sudo pip install fabric")


def install_jenkins():
    run("wget -q -O - https://jenkins-ci.org/debian/jenkins-ci.org.key | sudo apt-key add -")
    sudo("sh -c 'echo deb http://pkg.jenkins-ci.org/debian binary/ > /etc/apt/sources.list.d/jenkins.list'")
    os_update()
    os_install("jenkins")
    msg_sleep(20, "for Jenkins to start")
    install_plugins()
    thin_backup_install()
    add_jenkins_user_group()
    put_config_file()
    restart()


def install_maven(version=""):
    os_install("maven%s" % version)


def os_install(name):
    sudo("apt-get install %s -y" % name)


def os_update():
    sudo("apt-get update")


def add_jenkins_user_group():
    sudo("sudo usermod -a -G shadow jenkins")


def put_config_file():
    put("%s/sysconf/%s/jenkins/config.xml" % (env.manageroot, env.target),"/var/lib/jenkins/.", use_sudo=True)


def restart():
    sudo("/etc/init.d/jenkins restart")


def jenkins_cli(command, param=None, name=None, deploy=True, restart_jenkins=False):
    if not exists("jenkins-cli.jar"):
        run("wget http://localhost:8080/jnlpJars/jenkins-cli.jar")  #http://oldci.lagrummet.se/jnlpJars/jenkins-cli.jar
    param_cmd = " %s" % param if param else ""
    name_cmd = " -name %s" % name if name else ""
    deploy_cmd = " -deploy" if deploy else ""
    restart_cmd = " -restart" if restart_jenkins else ""
    command_line = "java -jar jenkins-cli.jar -s http://localhost:8080/ %s%s%s%s " % (command, param_cmd, deploy_cmd, restart_cmd)
    #java -jar jenkins-cli.jar -s http://ci.lagrummet.se/ install-plugin SOURCE ... [-deploy] [-name VAL] [-restart]
    run(command_line)


def jenkins_cli_install_plugin(plugin_name):
    jenkins_cli("install-plugin", param=plugin_name)


def install_plugins():
    jenkins_cli_install_plugin("git")
    jenkins_cli_install_plugin("github")
    jenkins_cli_install_plugin("throttle-concurrents")
    jenkins_cli_install_plugin("parameterized-trigger")
    jenkins_cli_install_plugin("run-condition")
    jenkins_cli_install_plugin("text-finder")


def thin_backup_install():
    put("%s/sysconf/%s/jenkins/thinBackup.xml" % (env.manageroot, env.target),"/var/lib/jenkins/.", use_sudo=True)
    jenkins_cli_install_plugin("thinBackup")


@task
@roles('ci')
def thin_backup_copy_to_ftp():
    username = get_value_from_password_store(PASSWORD_FILE_FTP_USERNAME_PARAM_NAME, "rinfo")
    password = get_value_from_password_store(PASSWORD_FILE_FTP_PASSWORD_PARAM_NAME, "pwd")
    target_file_name = "THIN_BACKUP_CI_%s" % env.datestamp
    tmp_thin_backup_tar_pack_path = "/tmp/thinbackup/"

    create_path(tmp_thin_backup_tar_pack_path)

    #with cd("/var/lib/jenkins/thinbackup/")
    tar_and_ftp_push("jenkinsconfig", target_file_name, password, "/var/lib/jenkins/thinbackup/FULL*",
                     tmp_thin_backup_tar_pack_path, username)
#def tar_and_ftp_push(snapshot_name, name, password, source_tar_path, target_path, username, test=False, md5sum=True):

    clean_path(tmp_thin_backup_tar_pack_path)


@task
@roles('ci')
def thin_backup_restore_from_ftp(backup_file):
    username = get_value_from_password_store(PASSWORD_FILE_FTP_USERNAME_PARAM_NAME, "rinfo")
    password = get_value_from_password_store(PASSWORD_FILE_FTP_PASSWORD_PARAM_NAME, "pwd")
    tmp_thin_backup_tar_pack_path = "/tmp/thinbackup/"

    create_path(tmp_thin_backup_tar_pack_path)
    create_path("/var/lib/jenkins/thinbackup/")

    ftp_fetch_and_untar("jenkinsconfig", backup_file, tmp_thin_backup_tar_pack_path, "/var/lib/jenkins/thinbackup/", username, password)
    #def ftp_fetch_and_untar(snapshot_name, name, tmp_path, target_tar_unpack_path, username, password, test=False, is_local=False, md5sum=True):