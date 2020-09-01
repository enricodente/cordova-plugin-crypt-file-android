![](https://img.shields.io/github/issues/enricodente/cordova-plugin-crypt-file-android.svg)
![](https://img.shields.io/github/license/enricodente/cordova-plugin-crypt-file-android.svg)

# Cordova crypt file plugin for Android only
HTML source file is encrypted at build, and decrypted at runtime. Works with Android build only.

NPM repository:
https://www.npmjs.com/package/cordova-plugin-crypt-file-android

## Plugin Install
`cordova plugin add cordova-plugin-crypt-file-android`

## Why Android only?
This plugin was created to provide a temporary solution to the issue of cordova-plugin-crypt-file (made by tkaji) and cordova-plugin-crypto-file (fork by PeterHdd) with cordova-ios 6.0.0+, where CDVURLProtocol was removed, breaking the plugin for that platform. Right now the only way to use this for Android is to add the plugin when building Android, then removing it when building for iOS. This plugin disables its features for iOS making it easier to be kept on the Android build, where encryption is more important anyways.

## Important for 'cordova-plugin-ionic-webview' users
The file `IonicWebViewEngine.java` (that is inside the ionic webview plugin) needs to be modified for this plugin to work and for the source code to be encrypted.

### Steps:
1. After adding the cordova-plugin-ionic-webview, navigate to the following location:

For cordova-android@6 :

**platforms/android/src/com/ionicframework/cordova/webview/IonicWebViewEngine.java**

For cordova-android@7 :

**platforms/android/app/src/main/java/com/ionicframework/cordova/webview/IonicWebViewEngine.java**

2. Remove the following code:

```
    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    @Override
    public WebResourceResponse shouldInterceptRequest(WebView view, WebResourceRequest request) {
      super.shouldInterceptRequest(view,request);
      return localServer.shouldInterceptRequest(request.getUrl());
    }

    @TargetApi(Build.VERSION_CODES.KITKAT)
    @Override
    public WebResourceResponse shouldInterceptRequest(WebView view, String url) {
      return localServer.shouldInterceptRequest(Uri.parse(url));
    }
```
3. Add this instead:

```
    @Override
    public WebResourceResponse shouldInterceptRequest(WebView view, WebResourceRequest request) {
      return super.shouldInterceptRequest(view, request);
    }
 ```
 
## Configuration

You can also change the port in the `config.xml`, but it needs to be the same port as the one used in the "cordova-plugin-ionic-webview".
To change the port, do the following:

#### cryptoPort

```xml
<preference name="cryptoPort" value="8080" />
```

## Run the plugin
No need to do anything more than installing the plugin, then run:
`cordova build [ios / android]`
You can check the result by opening your generated APK file with an archiving tool and opening your JS files with an editor.

## Encryption file types

### Default
The following file types will be encrypted as default setting:

* .html
* .htm
* .js
* .css

### Edit encrypted file types

You can specify the encryption file types by editing `plugin.xml`.

**plugins/cordova-plugin-crypt-file/plugin.xml**

```
<cryptfiles>
    <include>
        <file regex="\.(htm|html|js|css)$" />
    </include>
    <exclude>
        <file regex="exclude_file\.js$" />
    </exclude>
</cryptfiles>
```

Specify the target file types as a regular expression.


## Based on the original cordova-crypt-file created by tkyaji and PeterHdd fork

https://github.com/tkyaji/cordova-plugin-crypt-file
https://github.com/peterhdd/cordova-plugin-crypto-file

## License
Apache version 2.0
