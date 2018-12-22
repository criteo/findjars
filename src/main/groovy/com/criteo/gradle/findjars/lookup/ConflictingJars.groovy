package com.criteo.gradle.findjars.lookup

class ConflictingJars {
    Set<String> conflictingJars
    int hash

    ConflictingJars(Set<String> conflictingJars) {
        this.conflictingJars = conflictingJars
        this.hash = String.join(";", conflictingJars.sort()).hashCode()
    }

    Set<String> getJars() {
        conflictingJars
    }

    @Override
    boolean equals(o) {
        if (this.is(o)) return true
        if (getClass() != o.class) return false
        ConflictingJars that = (ConflictingJars) o
        return conflictingJars.equals(that.conflictingJars)
    }

    @Override
    int hashCode() {
        this.hash
    }
}
