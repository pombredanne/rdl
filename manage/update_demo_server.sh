#!/bin/bash

fab target.demo app.main.setup app.service.setup app.checker.setup
fab target.demo app.admin.all app.main.install app.checker.install app.service.install

EXIT_STATUS=$?
if [ $EXIT_STATUS -ne 0 ];then
   echo "fabric returned $EXIT_STATUS! Exiting!"
   exit $EXIT_STATUS
fi

fab target.demo -R main server.restart_tomcat