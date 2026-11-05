#!/usr/bin/env bash
# Build the TyCHE BEAST package ZIP (Maven replacement for `ant package`).
# FALLBACK: `mvn package` builds the same ZIP via maven-antrun-plugin (dev: mvn package -Ddev); use this script if that fails.
#
# Usage: ./release.sh [--dev] [--skip-build] [-- <extra mvn arguments>]
#   --dev          dev build: version becomes <version.xml version>-<git short hash>[-dirty]-built-<MMddHHmm>
#                  (-dirty = uncommitted changes to tracked files)
#                  (like `ant -Ddev=true package`; the base version is kept so BEAST still sorts it
#                  between the previous release and this one)
#   --skip-build   reuse the existing target/TyCHE.package.jar instead of running Maven
#   --             everything after this goes to mvn, e.g.
#                  ./release.sh -- -Dbeast.version=2.8.0-beta7 -Dbeast.classic.version=1.7.0-beta1
#
# Output: target/dist/TyCHE.v<version>.zip  (flat ZIP, same layout as the Ant build:
#         lib/TyCHE.package.jar, TyCHE.src.jar, version.xml, fxtemplates/, examples/, LICENSE)
set -euo pipefail
cd "$(dirname "$0")"

DEV=0
SKIP_BUILD=0
MVN_ARGS=()
while [ $# -gt 0 ]; do
  case "$1" in
    --dev) DEV=1 ;;
    --skip-build) SKIP_BUILD=1 ;;
    --) shift; MVN_ARGS=("$@"); break ;;
    -h|--help) sed -n '2,13p' "$0"; exit 0 ;;
    *) echo "Unknown option: $1 (put mvn arguments after --)" >&2; exit 1 ;;
  esac
  shift
done

# --- package name and version come from version.xml (single source of truth) ---
read_attr() {
  perl -0ne 'if (/<package\b[^>]*\b'"$1"'\s*=\s*["\x27]([^"\x27]*)["\x27]/) { print $1; exit }' version.xml
}
NAME=$(read_attr name)
FILE_VERSION=$(read_attr version)
[ -n "$NAME" ] && [ -n "$FILE_VERSION" ] || { echo "ERROR: could not read package name/version from version.xml" >&2; exit 1; }

VERSION="$FILE_VERSION"
if [ "$DEV" = 1 ]; then
  HASH=$(git rev-parse --short @ 2>/dev/null || true)
  if [ -n "$HASH" ]; then
    DIRTY=""
    [ -n "$(git status --porcelain --untracked-files=no 2>/dev/null)" ] && DIRTY="-dirty"
    VERSION="${FILE_VERSION}-${HASH}${DIRTY}-built-$(date +%m%d%H%M)"
  else
    echo "WARNING: --dev requested but no git commit found; using $FILE_VERSION" >&2
  fi
fi
echo "Package: $NAME  version: $VERSION"

POM_VERSION=$(perl -0ne 'if (/<artifactId>tyche<\/artifactId>(?:\s|<!--.*?-->)*<version>([^<]*)<\/version>/s) { print $1; exit }' pom.xml)
if [ "$POM_VERSION" != "${FILE_VERSION}-SNAPSHOT" ]; then
  echo "WARNING: pom.xml version ($POM_VERSION) does not match version.xml ($FILE_VERSION); expected ${FILE_VERSION}-SNAPSHOT" >&2
fi

# --- build the jar ---
JAR="target/${NAME}.package.jar"
if [ "$SKIP_BUILD" = 0 ]; then
  # the pom also builds the ZIP during `mvn package` (maven-antrun-plugin); skip that here, this script does its own
  mvn clean package -Dmaven.test.skip=true -Dmaven.antrun.skip=true ${MVN_ARGS[@]+"${MVN_ARGS[@]}"}
fi
[ -f "$JAR" ] || { echo "ERROR: $JAR not found (build failed or skipped?)" >&2; exit 1; }

# sanity checks: an empty or non-modular jar is what silently breaks loading in BEAST
JAR_LIST=$(mktemp -t tycherelease.XXXXXX)
trap 'rm -f "$JAR_LIST"' EXIT
unzip -Z1 "$JAR" > "$JAR_LIST"
NCLASSES=$(grep -c 'tyche/.*\.class$' "$JAR_LIST" || true)
if [ "$NCLASSES" -eq 0 ]; then
  echo "ERROR: $JAR contains no tyche classes (compile step produced nothing); not packaging." >&2
  exit 1
fi
grep -q '^module-info\.class$' "$JAR_LIST" || \
  echo "WARNING: $JAR has no module-info.class; BEAST 3 may load TyCHE before beast-classic." >&2

# --- stage the package (same layout as the Ant build) ---
STAGE="target/package"
DIST="target/dist"
rm -rf "$STAGE"
mkdir -p "$STAGE/lib" "$STAGE/fxtemplates" "$STAGE/examples" "$DIST"
cp "$JAR" "$STAGE/lib/"
STAGE_ABS="$PWD/$STAGE"
( cd src && zip -qr "$STAGE_ABS/${NAME}.src.jar" . -i '*.java' '*.png' '*.xsl' )
cp fxtemplates/*.xml "$STAGE/fxtemplates/"
cp -R examples/. "$STAGE/examples/"
cp LICENSE "$STAGE/"

# version.xml for the package: drop providers for tyche.* classes that are not in the jar
# (e.g. BEAUti classes while they are not built), drop emptied <service> blocks,
# and set the (possibly dev) version.
awk -v list="$JAR_LIST" '
  BEGIN { while ((getline line < list) > 0) have[line] = 1 }
  /<provider[ \t]+classname=/ {
    match($0, /classname="[^"]*"/)
    cn = substr($0, RSTART + 11, RLENGTH - 12)
    path = cn; gsub(/\./, "/", path); path = path ".class"
    if (cn ~ /^tyche\./ && !(path in have)) { print "  version.xml: dropped provider not in jar: " cn > "/dev/stderr"; next }
  }
  { print }
' version.xml \
  | perl -0pe 's/\s*<service\b[^>]*>\s*<\/service>//g' \
  | perl -0pe 's/(<package\b[^>]*\bversion\s*=\s*)(["\x27])[^"\x27]*\2/${1}${2}'"$VERSION"'${2}/' \
  > "$STAGE/version.xml"

# --- zip (flat: version.xml and lib/ at the top level, as BEAST requires) ---
ZIP="$PWD/$DIST/${NAME}.v${VERSION}.zip"
rm -f "$ZIP"
( cd "$STAGE" && zip -qr "$ZIP" . )
echo
unzip -l "$ZIP"
echo
echo "Built: ${ZIP#$PWD/}"
