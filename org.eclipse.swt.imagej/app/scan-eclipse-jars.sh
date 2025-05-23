#!/bin/sh

ECLIPSE_PLUGINS="/Applications/Eclipse.app/Contents/Eclipse/plugins"

# Parallel lists (same order and count!)
LIB_KEYS="core.commands core.runtime equinox.common jface jface.text osgi swt swt.win32 swt.gtk swt.cocoa.x86_64 swt.cocoa.aarch64"
LIB_VALUES="org.eclipse.core.commands org.eclipse.core.runtime org.eclipse.equinox.common org.eclipse.jface org.eclipse.jface.text org.eclipse.osgi org.eclipse.swt org.eclipse.swt.win32.win32.x86_64 org.eclipse.swt.gtk.linux.x86_64 org.eclipse.swt.cocoa.macosx.x86_64 org.eclipse.swt.cocoa.macosx.aarch64"

# Convert to arrays
set -- $LIB_KEYS
KEYS=("$@")
set -- $LIB_VALUES
VALS=("$@")

echo "<properties>"
echo "  <eclipse.home>/Applications/Eclipse.app/Contents/Eclipse</eclipse.home>"

i=0
while [ $i -lt ${#KEYS[@]} ]; do
  key=${KEYS[$i]}
  lib=${VALS[$i]}
  jar=$(ls "$ECLIPSE_PLUGINS"/${lib}_*.jar 2>/dev/null | sort | tail -n 1)
  prop_name=$(echo "$key" | sed 's/\./_/g')
  if [ -f "$jar" ]; then
    suffix="_$(basename "$jar" | sed -E "s/${lib}_(.*)\.jar/\1/")"
    echo "  <${prop_name}.suffix>${suffix}</${prop_name}.suffix>"
  else
    echo "  <!-- ${lib} not found (${prop_name}.suffix) -->"
  fi
  i=$((i+1))
done

echo "</properties>"