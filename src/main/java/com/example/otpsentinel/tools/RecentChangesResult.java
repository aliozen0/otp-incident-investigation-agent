package com.example.otpsentinel.tools;

import java.util.List;
import java.util.Objects;

public record RecentChangesResult(List<ChangeEvent> changes) {

  public RecentChangesResult {
    Objects.requireNonNull(changes, "changes must not be null");
    changes = List.copyOf(changes);
  }
}
