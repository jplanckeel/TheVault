package domain

import "sort"

// ComputeStats computes min, max, median and count from a slice of prices.
// Returns zero-value PriceStats if prices is empty.
func ComputeStats(prices []float64) PriceStats {
	if len(prices) == 0 {
		return PriceStats{}
	}

	sorted := make([]float64, len(prices))
	copy(sorted, prices)
	sort.Float64s(sorted)

	n := len(sorted)
	var median float64
	if n%2 == 0 {
		median = (sorted[n/2-1] + sorted[n/2]) / 2
	} else {
		median = sorted[n/2]
	}

	return PriceStats{
		Min:    sorted[0],
		Max:    sorted[n-1],
		Median: median,
		Count:  n,
	}
}
