package org.archivekeep.app.core.persistence.platform

import org.archivekeep.files.driver.fixtures.FixtureRepoBuilder

fun FixtureRepoBuilder.photosAdjustmentA() {
    deletePattern("2024/5/5.JPG".toRegex())
    deletePattern("2024/5/7.JPG".toRegex())
    deletePattern("2024/5/12.JPG".toRegex())
    addStored("2024/5/5-special.JPG", "2024/5/5.JPG")
    addStored("2024/5/7-special.JPG", "2024/5/7.JPG")
    addStored("2024/5/12-special.JPG", "2024/5/12.JPG")
}

fun FixtureRepoBuilder.photosAdjustmentB() {
    deletePattern("2024/6/.*".toRegex())
    addStored("2024/4/2-previous-extra-copy.JPG", "2024/4/2.JPG")
}
