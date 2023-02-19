package printutil

import (
	"fmt"
	"testing"

	"gotest.tools/v3/assert"
)

func TestPathDiff(t *testing.T) {
	tests := []struct {
		old  string
		new  string
		want string
	}{
		{"a", "b", "a -> b"},
		{"old-name", "new-name", "old-name -> new-name"},
		{"same-dir/a", "same-dir/b", "same-dir/{a -> b}"},
		{"old-dir/a", "new-dir/a", "{old -> new}-dir/a"},
		{"old-dir/a", "new-dir/b", "{old -> new}-dir/{a -> b}"},
		{"same-base/old-dir/a", "same-base/new-dir/a", "same-base/{old -> new}-dir/a"},
		{"same-base/old-dir/a", "same-base/new-dir/b", "same-base/{old -> new}-dir/{a -> b}"},
		{"same-base/old-dir/same-sub/a", "same-base/new-dir/same-sub/a", "same-base/{old -> new}-dir/same-sub/a"},
		{"same-base/old-dir/same-sub/a", "same-base/new-dir/same-sub/b", "same-base/{old -> new}-dir/same-sub/{a -> b}"},
		{"same-base/sub-dir/a", "same-base/moved-deeper/sub-dir/a", "same-base/{ -> moved-deeper/}sub-dir/a"},
		{"a/b/c/d/file", "a/b/c/d/c/d/file", "a/b/c/d/{ -> c/d/}file"},
	}
	for ti, tt := range tests {
		t.Run(fmt.Sprintf("%d", ti), func(t *testing.T) {
			colorFunc := func(a string) string {
				return a
			}

			if got := PathDiff(tt.old, tt.new, colorFunc); got != tt.want {
				assert.Equal(t, tt.want, got)
			}
		})
	}
}

func TestPathDiff_WithColoring(t *testing.T) {
	tests := []struct {
		old  string
		new  string
		want string
	}{
		{"a", "b", "<<|a -> b|>>"},
		{"old-name", "new-name", "<<|old-name -> new-name|>>"},
		{"same-dir/a", "same-dir/b", "same-dir/<<|{a -> b}|>>"},
		{"old-dir/a", "new-dir/a", "<<|{old -> new}|>>-dir/a"},
		{"old-dir/a", "new-dir/b", "<<|{old -> new}|>>-dir/<<|{a -> b}|>>"},
		{"same-base/old-dir/a", "same-base/new-dir/a", "same-base/<<|{old -> new}|>>-dir/a"},
		{"same-base/old-dir/a", "same-base/new-dir/b", "same-base/<<|{old -> new}|>>-dir/<<|{a -> b}|>>"},
		{"same-base/old-dir/same-sub/a", "same-base/new-dir/same-sub/a", "same-base/<<|{old -> new}|>>-dir/same-sub/a"},
		{"same-base/old-dir/same-sub/a", "same-base/new-dir/same-sub/b", "same-base/<<|{old -> new}|>>-dir/same-sub/<<|{a -> b}|>>"},
	}
	for ti, tt := range tests {
		t.Run(fmt.Sprintf("%d", ti), func(t *testing.T) {
			colorFunc := func(a string) string {
				return fmt.Sprintf("<<|%s|>>", a)
			}

			if got := PathDiff(tt.old, tt.new, colorFunc); got != tt.want {
				assert.Equal(t, tt.want, got)
			}
		})
	}
}
