package home

type Model struct {
	LocalArchives []LocalArchiveStatusViewModel

	ConnectedExternalStorageStatus    []ExternalStorageStatus
	DisconnectedExternalStorageStatus []ExternalStorageStatus
}

type LocalArchiveStatusViewModel struct {
	Title string

	TotalFiles     int
	UntrackedFiles int

	SyncStrategyCoverageEntries []LocalArchiveSyncStrategyCoverageEntryViewModel
}

type LocalArchiveSyncStrategyCoverageEntryViewModel struct {
	RemoteStorageName string

	Percentage float64

	Description string
}

type ExternalStorageStatus struct {
	Name string

	TotalSpaceUsed           int64
	TotalSpaceUsedByArchives int64
	TotalSpace               int64
	TotalFreeSpace           int64

	Archives []interface{}
}
