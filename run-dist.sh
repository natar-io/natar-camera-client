
CP=$(</usr/share/natar/natar-camera-client/classpath.txt)
java -Xmx64m -cp $CP:target/* tech.lity.rea.nectar.CameraTest --input camera0 


