#!/bin/sh

fab target.prod -R doc app.docs.build app.docs.deploy

EXIT_STATUS=$?
if [ $EXIT_STATUS -ne 0 ];then
   echo "fabric returned $EXIT_STATUS! Exiting!"
   exit $EXIT_STATUS
fi