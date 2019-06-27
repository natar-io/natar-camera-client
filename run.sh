
CP=$(<classpath.txt)
java -Xmx64m -cp $CP:target/* tech.lity.rea.nectar.CameraTest $@


