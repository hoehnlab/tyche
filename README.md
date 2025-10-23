# TyCHE

TyCHE is a Bayesian phylogenetics BEAST2 package that infers time trees of populations with distinct evolutionary rates. Mutation rates often vary dramatically by cell type, and TyCHE improves accuracy of trees for these heterogenously evolving populations by simultaneously reconstructing ancestral cell types and inferring the time tree by linking those cell types to mutation rates for each branch.

TyCHE stands for Type-linked Clocks for Heterogenous Evolution, and is named in honor of the Greek goddess of chance in recognition of the stochastic nature of Bayesian analysis.

:globe_with_meridians: [TyCHE website](https://tyche.readthedocs.io)

## Citation

To cite the TyCHE package in publications, please use:

Fielding J, Wu S, Melton H, Du Plessis L, Fisk N, Hoehn K (2025). "TyCHE enables time-resolved lineage tracing of heterogeneously-evolving populations." bioRxiv 2025.10.21.683591 doi:10.1101/2025.10.21.683591 https://doi.org/10.1101/2025.10.21.683591

## Getting started

A full tutorial, including installation and recommended usage with Dowser, is available at Dowser's [Build Time Trees with TyCHE vignette](https://dowser.readthedocs.io/en/latest/vignettes/Building-Time-Trees-Vignette/).

A BEAUti template is currently in development. 🏗️

For full API reference, visit [tyche.readthedocs.io](https://tyche.readthedocs.io).


## Install TyCHE and dependecies

#### For Mac and Windows machines, we recommend:

1.
     <br/>
     <b>Mac</b>:     Click to <a href="https://github.com/CompEvol/beast2/releases/download/v2.7.7/BEAST.v2.7.7.Mac.dmg">download the BEAST 2.7.7 dmg</a>. Open the dmg file and drag the BEAST application to your Applications folder. <br/>
     <b>Windows</b>: Click to <a href="https://github.com/CompEvol/beast2/releases/download/v2.7.7/BEAST.v2.7.7.Windows.zip">download the BEAST 2.7.7 zip</a>. Right click on the zip file to extract the BEAST folder.<br/>
   <b>OR</b> download the appropriate version from <a href="https://github.com/CompEvol/beast2/releases/tag/v2.7.7">https://github.com/CompEvol/beast2/releases/tag/v2.7.7</a> or <a href="https://www.beast2.org">www.beast2.org</a>.

2. Open BEAUti, click on the "File" menu, and select "Manage Packages...".

3. In the package manager, find and install the "BEAST Classic" package.

4. Follow this tutorial to install the TyCHE package from this repository "by hand" (see [releases](/releases):
[www.beast2.org/managing-packages](https://www.beast2.org/managing-packages/index.html)

5. Follow this tutorial to install the [rootfreqs package](https://github.com/rbouckaert/rootfreqs) "by hand":
[www.beast2.org/managing-packages](https://www.beast2.org/managing-packages/index.html)

#### For Linux machines, we recommend running:
```
# Choose appropriate version for your architecture (x86 or aarch64)
BEAST=BEAST.v2.7.7.Linux.x86.tgz # or BEAST=BEAST.v2.7.7.Linux.aarch64.tgz

# download file and uncompress
curl -O https://github.com/CompEvol/beast2/releases/download/v2.7.7/$BEAST
tar -xvzf $BEAST

# optionally remove the compressed file
rm $BEAST

# run BEAST, at least with help, to allow it to set up its directories
~/beast/bin/beast -help

# install BEAST Classic package
~/beast/bin/packagemanager -add BEAST_CLASSIC

# install TyCHE package (currently not released on BEAST package manager)
curl -O https://github.com/hoehnlab/tyche/releases/download/v0.0.3/TyCHE.v0.0.3.zip 
unzip -o -d ~/.beast/2.7/TyCHE TyCHE.v0.0.3.zip
rm -f TyCHE.v0.0.3.zip

# install rootfreqs package
ROOTFREQS=rootfreqs.package.v0.0.2.zip
curl -O https://github.com/rbouckaert/rootfreqs/releases/download/v0.0.2/$ROOTFREQS
unzip -o -d ~/.beast/2.7/rootfreqs $ROOTFREQS
rm -f $ROOTFREQS

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

