#!/bin/sh
MYSELF=`which "$0" 2>/dev/null`
[ $? -gt 0 -a -f "$0" ] && MYSELF="./$0"
java=java
if test -n "$JAVA_HOME"; then
    java="$JAVA_HOME/bin/java"
else
    echo "Please Set  JAVA_HOME"
fi
exec "$java" $java_args -cp ./lib/ -jar $MYSELF "$@"
exit 1 
