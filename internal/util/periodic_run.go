package util

import (
	"context"
	"time"
)

func RunPeriodicallyAndOnceAfterDone(
	ctx context.Context,
	period time.Duration,
	f func() error,
) error {
	ticker := time.NewTicker(period)
	defer ticker.Stop()

	for running := true; running; {
		select {
		case <-ctx.Done():
			running = false
		case <-ticker.C:
		}

		saveStart := time.Now()

		err := f()
		if err != nil {
			return err
		}

		executionDuration := time.Now().Sub(saveStart)
		if executionDuration > period {
			period = executionDuration * 2
			ticker.Reset(period)
		}
	}

	return nil

}
