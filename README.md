# TyCHE

## To Build This Package locally (recommended for developers)

First you will need to make sure you have beast2, BeastFX, and beast-classic 
cloned in the same directory as this repo. 

Recommended file structure:
```txt
parent-dir
|___beast2
|___BeastFX
|___beast-classic
|___tyche
```

For convenience, here are the commands to clone these repos:
```sh
cd <parent-dir>
git clone https://github.com/CompEvol/beast2.git
git clone https://github.com/CompEvol/BeastFX.git
git clone https://github.com/BEAST2-Dev/beast-classic.git
git clone <this-repo>
```

Make sure you have `ant` installed. Set `JAVACMD` env variable to the path for the [JDK recommended for beast2 development](https://github.com/CompEvol/BeastFX/blob/master/DevGuideIntelliJ.md#azul-jdk-17). (See [here](https://www.beast2.org/package-development-guide/) for a general guide to Beast2 development.)

Now, you can run these commands to build all necessary jar files and then 
package files:
 
```sh
cd <parent-dir>/beast2
ant build_jar_all_BEAST_NoJUnitTest
cd ../BeastFX
ant build_jar_all_BeastFX_NoJUnitTest
cd ../beast-classic
ant build_jar_all_BEAST_CLASSIC_NoJUnitTest
cd ../tyche
ant package
```

