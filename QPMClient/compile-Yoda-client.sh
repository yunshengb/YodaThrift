printdivision()
{
	echo -e "\n"
	for i in `seq 1 70`; do
		echo -n "/";
	done
	echo -e "\n"
}

# Generate thrift files
echo -e "./compile-Yoda-client.sh: `pwd`"
echo -e "./compile-Yoda-client.sh: Compiling thrift source code..."
thrift --gen java QPM.thrift
printdivision

# Add jar files to class path
export JAVA_CLASS_PATH=$JAVA_CLASS_PATH:lib/commons-lang3-3.4.jar:lib/indri.jar:lib/jsoup-1.8.3.jar:lib/libthrift-0.9.2.jar:lib/slf4j-api-1.7.13.jar:lib/slf4j-simple-1.7.13.jar

# Use cp flag to avoid cluttering up the CLASSPATH environment variable
echo -e "javac -cp $JAVA_CLASS_PATH QPMClient.java gen-java/QPMstubs/QPM.java 
\n\n"
javac -cp $JAVA_CLASS_PATH QPMClient.java gen-java/QPMstubs/QPM.java 
