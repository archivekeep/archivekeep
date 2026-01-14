---
---

<div class="header">

<h1>Archive Keep</h1>

<a href="{{< ref "/about" >}}">Docs</a>
[GitHub](https://github.com/archivekeep/archivekeep)

</div>

<div class="main-display-container">
    <div class="main-display-inner-container">
        <div class="text">
            <h2>ArchiveKeep<span class="sub">Personal files archivation</span></h2>
            <p>Use your own <strong>SSD, HDD, USB flash-drives,...</strong> Rent <strong>cloud storages</strong> without vendor lock-in. Self-host. <strong>Combine</strong> storages.</p>
            <div class="links">
                <a href='https://flathub.org/apps/org.archivekeep.ArchiveKeep'><picture><source type="image/svg+xml" srcset="https://flathub.org/api/badge?svg&locale=en"><img width="180" alt="Get it on Flathub" srcset="https://flathub.org/api/badge?locale=en 240w" src="https://flathub.org/api/badge?locale=en"/></picture></a>
                <a class="other-link" href="{{< ref "/install" >}}">Or, install from sources</a>
            </div>
        </div>
        <div class="other">
        <img src="/generated_screenshots/desktop/main-window.png" />
        </div>
    </div>

<div>

{{< hint warning >}}
<p><strong>This software is under development.</strong></p>
<p>It serves <a href="https://kravemir.org">author's</a> needs. However, there's still a room for improvement.
<p>Consider buying author a pizza to support the development.</p>
{{< /hint >}}

</div>
</div>


<div class="sub-display-container">
<div class="sub-display-inner-container">
<div class="text">

## Plain files

Access files directly as ordinary files.

Devices can read your files without having to install any extra software.

The archivekeep only facilitates archivation process to help you replicate your data for preservation.
</div>
<div class="other small">
<img src="/generated_screenshots/dialogs/add-and-push/example.png" />
</div>
</div>
<div class="sub-display-inner-container">
<div class="other small">
<img src="/generated_screenshots/dialogs/sync/upload-example.png" />
</div>
<div class="text">

## Offline first

Incrementally synchronize data to other media, when it is connected.

Store copies on your SSD, HDD, USB flash or other media of your choice. No cloud required.

_**Note:** encrypt your drives if you want to [ensure privacy of your data at rest](https://en.wikipedia.org/wiki/Data_at_rest#Encryption)._

</div>
</div>
<div class="sub-display-inner-container">
<div class="text">

## Nondestructive replication

The replication process prevents propagation of redactions, deletions, corruption or other destructive changes in files.

The contents of files stored in repositories are verifiable using digistal checksums.
</div>
<div class="other small">
<img src="/generated_screenshots/dialogs/sync/download-example.png" />
</div>
</div>
</div>

<div class="sub-display-container" style="background: white">
<div class="sub-display-inner-container">
<div class="text">

## Cloud supported

For data preservation confidence, replication to remote storage(s) is essential.

Supported API:

- S3 API - choose vendor(s) you like, no lock-in.


</div>
<div class="other small">
</div>
</div>
<div class="sub-display-inner-container">
<div class="other small">
</div>
<div class="text">

## Optional E2E encryption

Optional E2E encryption provides ability to use untrusted media and services more securely, such as:

* SD cards, USB flash-drives,... and other filesystems without encryption,
* S3 repositories in the cloud.

_**Notice:** only file contents are encrypted, not filenames._

</div>
</div>
<div class="sub-display-inner-container">
<div class="text">

## Self-hosting friendly

If you like to have it all in your own hands. You can self-host the storages and access them using standard API(s).

_**Note:** there's no dedicated application for self-hosting._ 

</div>
<div class="other small">
</div>
</div>
</div>

<div class="sub-display-container">
<div class="sub-display-inner-container">
<div class="text">

## Android platform

<p>Carry a copy of data in mobile.</p>
<p><a class="other-link" href="https://f-droid.org/packages/org.archivekeep.ArchiveKeep">Get it on F-Droid.</a></p>
<p><a class="other-link" href="https://github.com/archivekeep/archivekeep/releases/">Or, download from releases.</a></p>


</div>
<div class="other small">
<img src="/generated_screenshots/mobile/main-window.png" />
</div>
</div>
</div>

<div class="three-col-section-wrapper">
<div class="three-col-section">
<div><div>


## Pricing

This is an open-source software, and it is free and will be free, to use the tool and self-host a server.

The source code is available on [github](https://github.com/archivekeep/archivekeep).


</div></div>
<div><div>

## Warranty

Although **author desires** to make the system 100% bulletproof **to prevent loss of own data**.

This is open-source software, and it is made **available for free, and comes with no warranty**.

</div></div>
<div><div>

## License

The project is licensed under **AGPL v3**.

</div></div>
</div></div>

[LUKS]: https://en.wikipedia.org/wiki/Linux_Unified_Key_Setup
