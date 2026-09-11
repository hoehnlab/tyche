.. _installation_guide:


For Mac and Windows machines, we recommend:
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

.. raw:: html

    <style>
    .rst-content section ul.os {
        list-style: none;
        list-style-type: none;
    }
    .rst-content section .os li {
        list-style: none;
        list-style-type: none;
        position: relative;
        padding-left: 6em;
    }
    .os li:before{
        position: absolute;
        left: 0;
        width: 6em;
        text-align: left;
    }
    li.mac:before {
        content: "Mac:";
        font-weight: bold;
    }
    li.windows:before {
        content: "Windows:";
        font-weight: bold;
    }
    </style>
    <ol>
    <li>
        <ul class="os">
        <br/>
        <li class="mac" style="">Click to <a href="https://github.com/CompEvol/beast2/releases/download/v2.7.7/BEAST.v2.7.7.Mac.dmg">download the BEAST 2.7.7 dmg</a>. Open the dmg file and drag the BEAST application to your Applications folder. </li>
        <li class="windows">Click to <a href="https://github.com/CompEvol/beast2/releases/download/v2.7.7/BEAST.v2.7.7.Windows.zip">download the BEAST 2.7.7 zip</a>. Right click on the zip file to extract the BEAST folder. </li>
        </ul>
    <b>OR</b> download the appropriate version from <a href="https://github.com/CompEvol/beast2/releases/tag/v2.7.7">https://github.com/CompEvol/beast2/releases/tag/v2.7.7</a>
    or <a href="https://www.beast2.org">www.beast2.org</a>.
    </li>
    </ol>


2. Open BEAUti, click on the "File" menu, and select "Manage Packages...".

3. In the package manager, find and install the "BEAST Classic" package.

4. Follow this tutorial to add the "extra packages" package repository (use https://github.com/CompEvol/CBAN/blob/master/packages-extra-2.7.xml as the package repository URL):
   `www.beast2.org/managing-packages <https://www.beast2.org/managing-packages/index.html>`_

5. In the package manager, find and install the "TyCHE" package. This tutorial relies on version v0.0.10 or later.

6. In the package manager, find and install the "rootfreqs" package.


For Linux machines, we recommend running:
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

.. code-block:: bash

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

    # add "extra packages" package repo
    echo "packages.url=https\://raw.githubusercontent.com/CompEvol/CBAN/master/packages-extra-2.7.xml" >> ~/.beast/2.7/beauti.properties

    # install TyCHE package
    ~/beast/bin/packagemanager -add TyCHE

    # install rootfreqs package
    ~/beast/bin/packagemanager -add rootfreqs

