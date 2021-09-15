#!/bin/sh
installDir="/usr/share/universal"
if [ -d "\$installDir" ]
then
  rm -r "\$installDir"
fi
mkdir "\$installDir"
if [ \$? -ne 0 ]
then
  installDir="\$HOME/.local/share/universal"
  if [ -d "\$installDir" ]
  then
    rm -r "\$installDir"
  fi
  mkdir "\$installDir"
  if [ \$? -ne 0 ]
  then
    echo "Could not create install directory. Try running with sudo" 1>&2
    exit 1
  fi
fi

mkdir "\$installDir/firefox"
if [ \$? -ne 0 ]
then
  echo "Could not create browser directory" 1>&2
  exit 1
fi
# Install Firefox
if [ ! -f "/tmp/firefox.tar.bz2" ]
then
  firefoxDownload="https://download-installer.cdn.mozilla.net/pub/${firefox_module}/releases/${firefox_revision}/linux-x86_64/en-US/firefox-${firefox_revision}.tar.bz2"
  echo "Downloading Firefox"
  if [ `command -v wget` ]
  then
    wget -O "/tmp/firefox.tar.bz2" "\$firefoxDownload"
  elif [ `command -v curl` ]
  then
    curl -o "/tmp/firefox.tar.bz2" "\$firefoxDownload"
  fi
  if [ \$? -ne 0 ]
  then
    echo "Could not download Firefox from \$firefoxDownload" 1>&2
    exit 2
  fi
fi

cd "\$installDir"
echo "Installing Firefox"
tar -xj -f "/tmp/firefox.tar.bz2"
if [ \$? -ne 0 ]
then
  echo "Could not extract firefox" 1>&2
  exit 3
fi
chmod +X "\$installDir/firefox/firefox"
rm "/tmp/firefox.tar.bz2"
if [ \$? -ne 0 ]
then
  echo "WARNING: Could not clean up firefox!" 1>&2
fi

cd "\$OLDPWD"
cd "\$installDir/firefox"
# Install browser configurations
echo "Extracting browser configuration"
echo "Loading browser configuration"
confIndex=`awk '/^__BROWSERCONF__/ {print NR + 1; exit 0;}' \$0`
appIndex=`awk '/^__APPDATA__/ {print NR + 1; exit 0;}' \$0`
confLength=`expr \$appIndex - 1 - \$confIndex`
tail -n+\$confIndex \$0 | head -n-\$confLength | tar -x

mkdir -p "\$HOME/.mozilla/native-messaging-hosts"
echo "Installing native app"
cat >"\$HOME/.mozilla/native-messaging-hosts/universalmusic.json" << ENDNATIVE
{
  "name": "universalmusic",
  "description": "A link between the Universal Music Player and the web browser.",
  "path": "\$installDir/bin/addonInter",
  "type": "stdio",
  "allowed_extensions": ["universalmusic@regis.edu"]
}
ENDNATIVE
cd "\$OLDPWD"

# Install the actual application
cd "\$installDir"
echo "Copying application data"
echo "Extracting application data"
tail -n+\$appIndex \$0 | tar -x
chmod +X "\$installDir/bin/interface"
chmod +X "\$installDir/bin/addonInter"

cd "\$OLDPWD"
ln -fs "\$installDir/bin/interface" "\$installDir/../../bin/universalplayer"
echo "Done!"
exit 0

