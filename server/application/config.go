package application

type Config struct {
	GRPC GRPCConfig
	HTTP HTTPConfig
}

type GRPCConfig struct {
	Enable bool
}

type HTTPConfig struct {
	Enable bool

	PublicPath string
}
