package domain_test

import (
	"testing"

	"github.com/jplanckeel/thevaultbo/internal/domain"
)

func TestComputeStats_Normal(t *testing.T) {
	prices := []float64{10.0, 20.0, 30.0, 40.0, 50.0}
	stats := domain.ComputeStats(prices)

	if stats.Min != 10.0 {
		t.Errorf("Min: want 10.0, got %f", stats.Min)
	}
	if stats.Max != 50.0 {
		t.Errorf("Max: want 50.0, got %f", stats.Max)
	}
	if stats.Median != 30.0 {
		t.Errorf("Median: want 30.0, got %f", stats.Median)
	}
	if stats.Count != 5 {
		t.Errorf("Count: want 5, got %d", stats.Count)
	}
}

func TestComputeStats_Even(t *testing.T) {
	prices := []float64{10.0, 20.0, 30.0, 40.0}
	stats := domain.ComputeStats(prices)
	if stats.Median != 25.0 {
		t.Errorf("Median (even): want 25.0, got %f", stats.Median)
	}
}

func TestComputeStats_Empty(t *testing.T) {
	stats := domain.ComputeStats(nil)
	if stats.Count != 0 {
		t.Errorf("Count: want 0, got %d", stats.Count)
	}
}

func TestComputeStats_Single(t *testing.T) {
	stats := domain.ComputeStats([]float64{42.0})
	if stats.Min != 42.0 || stats.Max != 42.0 || stats.Median != 42.0 {
		t.Errorf("Single: want all 42.0, got %+v", stats)
	}
}
