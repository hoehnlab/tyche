# TyCHE


## Install TyCHE

Install BEAST2, current recommended version is [2.7.7](https://github.com/CompEvol/beast2/releases/tag/v2.7.7)

On Linux, download [BEAST 2.7.7 here](https://github.com/CompEvol/beast2/releases/download/v2.7.7/BEAST.v2.7.7.Linux.x86.tgz).
Unzip the .tgz using 
```sh
tar -xzvpf BEAST.v2.7.7.Linux.x86.tgz
```

Install beast-classic:
```sh
~/beast/bin/packagemanager -add BEAST_CLASSIC
```

[Download](https://github.com/hoehnlab/tyche/releases/download/v0.0.1/TyCHE.v0.0.1.zip) and install TyCHE:
```sh
unzip -d ~/.beast/2.7/TyCHE <path>/TyCHE.v0.0.1.zip
```

### To make use of Dowser's full set of features (in development)
Checkout and locally install [immcantation:dowser/bug_fixes](https://github.com/immcantation/dowser/tree/bug_fixes)

Checkout our [xml templates](https://github.com/hoehnlab/xml-writer)

[Download](https://github.com/rbouckaert/rootfreqs/releases/download/v0.0.2/rootfreqs.package.v0.0.2.zip) and install rootFreqs:
```sh
unzip -d ~/.beast/2.7/rootfreqs <path>/rootfreqs.package.v0.0.2.zip
```

----

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

