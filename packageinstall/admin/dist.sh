
rm -rf tmp
mkdir -p tmp/output

cp ../../manage/sysconf/template/www/robots.txt tmp/

cp ../../manage/sysconf/common/etc/apache2/conf.d/jk.conf tmp/

sed 's/dnsplaceholderforsed/'$1'/g' ../../manage/sysconf/template/etc/apache2/sites-available/admin > tmp/admin

cp ~/.ssh/id_rsa.pub tmp/

cp install.sh tmp/

( cd ../../tools/rinfomain &&
	groovy base_as_feed.groovy -b ../../resources/base/ -s ../../resources/template/datasources.n3 -o ../../packageinstall/admin/tmp/output
)

( cd tmp && tar -zcf ../rinfo-admin.tar.gz * )

