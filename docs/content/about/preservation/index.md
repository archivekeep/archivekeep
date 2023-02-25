---
title: 'Preservation'
weight: 10
---

Preservation of files is one of the main design goals of ArchiveKeep.

There are many possible causes of data loss. The most common ones are:

- hardware failure resulting in complete or partial loss of data on one device,
- silent hardware failure resulting in partial data loss,
- silent ransomware resulting in encryption of files,
- propagation of corrupted data or file deletions to other copies.

## Single point of failure (SPOF)

Single point of failure is a part of system, that if fails, everything stops from working.

In context of file archivation, a failure of a single point of failure could result in complete data loss.

### Single storage device as a SPOF

In case, that all copies of the data is being kept in a single device.
If that device fails, then all data, and all copies of the same data, on this device could be lost.

I.e. power supply failure, or high over-voltage in electrical grid, could result in all drives in the same device to be fried.
This includes also all drives in a single RAID device.  

### Laptop as a SPOF

Usually, laptop is a primary device, which stores all the data and has got full access to cloud services and other online backup storages.
Therefore, laptop provides: 

- access to all data on the device,
- access to all logged-in sessions to cloud services,
- access to all logged-in sessions to online backup hosting.

If a laptop was compromised, then a malicious actor, or a malware, could wipe out or encrypt all the&nbsp;data, that is accessible from the laptop.

### Home as a SPOF

In case, that all copies of the data is being kept in a single building. If that building is destroyed, then all data is destroyed.

I.e. home catches fire, or a natural disaster would happen. 
People naturally seek to save their own and other lives, 
and (scans of) important documents are lost, plus all the family photos and videos. 

## Silent loss of data

Silent loss of data can happen, if all copies of data were made from already corrupted data.

The data can be silently corrupted, or silently lost, by different causes, such as:

- bit flipping error,
- silent ransomware quietly encrypting data,
- malicious actor deleting some files.

The worst part about silent data loss, is that you realize something is missing, when you need it.

### Silent loss of data in rotated backups

Backups create copies of data at given point in time, and provide ability to go back in time.
However, the storage capacity isn't unlimited, and rotation of backups is one of techniques to deal with the problem of limited capacity.

Rotated backup approach erases an old backup to free space for a new backup to be created.

_There are other backup solutions, that retain full history without duplication of same data,
and therefore protect against this kind of data loss.
However, it gets complicated, when data owner modifies or deletes some files on purpose, and wants to redact such backups._

### Silent data loss in replicas

Data replication ensures storage redundancy of by keeping multiple copies of live data, which are exactly same.
This helps to protect from hardware failure, or natural disasters.

The issue with replication is, that it propagates all changes, including deletion or modification of files introducing corruption and data loss. 
