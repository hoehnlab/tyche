# Configuration file for the Sphinx documentation builder.
#
# For the full list of built-in configuration values, see the documentation:
# https://www.sphinx-doc.org/en/master/usage/configuration.html

# -- Project information -----------------------------------------------------
# https://www.sphinx-doc.org/en/master/usage/configuration.html#project-information

import os
import sys
import re

project = 'TyCHE'
copyright = '2025, Jessie Fielding'
author = 'Jessie Fielding'
release = re.sub('^v', '', os.popen('git describe --tags').read().strip())
# release = "0.0.3"

# -- General configuration ---------------------------------------------------
# https://www.sphinx-doc.org/en/master/usage/configuration.html#general-configuration

import sys
from pathlib import Path

conf_dir = Path(__file__).parent  # docs/source, wherever it's actually invoked from

sys.path.append(str(conf_dir / '_ext'))

extensions = [
    'java_domain',
    'java_documenter'
]

templates_path = ['_templates']
exclude_patterns = []

java_documenter_source = str((conf_dir / '..' / '..' / 'src').resolve())
toc_object_entries = True

html_sidebars = { '**': ['globaltoc.html', 'relations.html', 'sourcelink.html', 'searchbox.html'] }
html_favicon = '_static/tyche_logo_transparent.png'


# -- Options for HTML output -------------------------------------------------
# https://www.sphinx-doc.org/en/master/usage/configuration.html#options-for-html-output

html_theme = 'sphinx_rtd_theme'
html_static_path = ['_static']
