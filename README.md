# VideoEncryptedPlayer
Encrypted video player based on Android VideoView

## Encryption
In the process of downloading the video stream encryption, Sample encryption method is in the beginning of the file stream embedded encryption characters.

## Decrypt
Playing encrypted video, through the HttpServer parsing encrypted files, skip the embedded encryption characters, and input stream to the video player to play.

## Other
There are many ways to encrypt the file stream, and you can choose the encryption method yourself, such as DES encryption ... and you need to decrypt it yourself.

![sample](https://github.com/Liar1995/VideoEncryptedPlayer/blob/master/sample.png)
