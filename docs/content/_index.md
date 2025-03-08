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
            <h2>Personal files archivation<span class="sub">Under your uncompromised control</span></h2>
            <p>No vendor lock-in. Use your own storages, rent hosted or cloud storages from vendors of your choice, or self-host. And, combine them as you want.</p>
            <p><a href="{{< ref "/install" >}}">Install</a></p>
        </div>
        <div class="other">
        </div>
    </div>

<div>
{{< hint warning >}}
This software is still under development.
{{< /hint >}}
</div>
</div>


<div class="sub-display-container">
<div class="sub-display-inner-container">
<div class="other">

</div>
<div class="text">

## Plain files

Access your archived files directly without any external tool.

The archivekeep only facilitates archivation process to help you replicate your data for preservation.
</div>
</div>
<div class="sub-display-inner-container">
<div class="other">
<img src="/generated_screenshots/dialogs/add-and-push/example.png" />
</div>
<div class="text">

## Offline first

<p>Offline storages are not always online, because they are offline. And, drives aren't a computer.</p>
<p>The archivekeep is designed for such asynchronous approach of synchronization in repositories with archive replicas.</p>


## Nondestructive replication

The replication process prevents propagation of redactions, deletions, corruption or other destructive changes in files.

The contents of files stored in repositories are verifiable using digistal checksums.
</div>
</div>
</div>


<div class="sub-display-container" style="background: white">
<div class="sub-display-inner-container">
<div class="text" style="flex: 3 0 0">

## Features

Supported storages:

- Internal or external SSD, HDD, USB flash or other file-systems,
- _(planned)_ Phone,
- _(planned)_ Amazon S3-compatible API, 
- _(planned)_ self-hosting.

Encryption options:

- No encryption,
- Transparent filesystem level encryption (i.e. [LUKS]),
- _(planned)_ E2E.
</div>
<div class="other">
</div>
</div>
</div>


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
</div>

[LUKS]: https://en.wikipedia.org/wiki/Linux_Unified_Key_Setup
