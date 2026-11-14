package domain

import (
	"testing"
	"time"
)

const backup = `{"version":1,"items":[
 {"kind":"game","title":"Zelda","platform":"GameCube","status":"OWNED","value":40,"notes":"secret","purchasePrice":10,"location":"cave"},
 {"kind":"game","title":"Pikmin","platform":"GameCube","status":"WISHLIST","value":25},
 {"kind":"game","title":"Mario","platform":"GameCube","status":"SOLD","value":5}]}`

func TestBuildPublicView(t *testing.T) {
	v, err := BuildPublicView(Share{Scope: ShareCollection, Title: "Ma collection"}, []byte(backup), time.Now())
	if err != nil {
		t.Fatal(err)
	}
	if v.Count != 1 || v.Items[0].Title != "Zelda" || v.Total == nil || *v.Total != 40 {
		t.Fatalf("unexpected view %+v", v)
	}
	hidden, _ := BuildPublicView(Share{Scope: ShareWishlist, HidePrices: true}, []byte(backup), time.Now())
	if hidden.Count != 1 || hidden.Items[0].Value != nil || hidden.Total != nil {
		t.Fatalf("prices should be hidden: %+v", hidden)
	}
}

func TestValidateBackupAndShare(t *testing.T) {
	if ValidateBackup([]byte(backup)) != nil {
		t.Error("valid backup rejected")
	}
	for _, bad := range []string{`[]`, `{}`, `nope`} {
		if ValidateBackup([]byte(bad)) == nil {
			t.Errorf("%s should be rejected", bad)
		}
	}
	if ValidateShare("everything", "") == nil || ValidateShare(ShareWishlist, "Mes envies") != nil {
		t.Error("share validation")
	}
}
