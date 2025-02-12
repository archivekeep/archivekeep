package org.archivekeep.app.core.persistence.platform.demo

import org.archivekeep.testing.fixtures.FixtureRepoBuilder

val documentsContents: FixtureRepoBuilder.() -> Unit = {
    ('A'..'Z').forEach { category ->
        (1..(3451235 % ((category.code) % 33 + 22))).forEach { file ->
            addStored("$category/$file.PDF")
        }
    }
    (2024..2024).forEach { year ->
        (1..7).forEach { month ->
            (1..(50 % (month + 8))).forEach {
                addStored("$year/$month/$it.PDF")
            }
        }
    }

    addUncommitted("2024/8/1.JPG")
    addUncommitted("2024/8/2.JPG")
}

val photosBaseContents: FixtureRepoBuilder.() -> Unit = {
    (2010..2023).forEach { year ->
        (1..12).forEach { month ->
            (1..(43312 % (month + 32))).forEach {
                addStored("$year/$month/$it.JPG")
            }
        }
    }
    (2024..2024).forEach { year ->
        (1..7).forEach { month ->
            (1..(12342 % (month + 44))).forEach {
                addStored("$year/$month/$it.JPG")
            }
        }
    }

    (2024..2024).forEach { year ->
        (8..8).forEach { month ->
            (1..20).forEach {
                addUncommitted("$year/$month/$it.JPG")
            }
        }
    }
}

val booksBaseContents: FixtureRepoBuilder.() -> Unit = {
    ('A'..'Z').forEach { category ->
        (1..(325245324 % ((category.code) % 10 + 3))).forEach { file ->
            addStored("Genre $category/$file.EPUB")
        }
    }
}

val musicBaseContents: FixtureRepoBuilder.() -> Unit = {
    ('A'..'Z').forEach { category ->
        (1..(325245324 % ((category.code) % 10 + 9))).forEach { file ->
            addStored("Genre $category/$file.ogg")
        }
    }
}

val privateBaseContents: FixtureRepoBuilder.() -> Unit = {
    ('A'..'Z').forEach { category ->
        (1..(234523345 % ((category.code) % 17 + 11))).forEach { file ->
            addStored("Private category $category/$file.PDF")
        }
    }
}

val productionsBaseContents: FixtureRepoBuilder.() -> Unit = {
    ('A'..'F').forEach { domain ->
        ('I'..'O').forEach { materialType ->
            (2018..2024).forEach { year ->
                (1..(435241 % ((domain.code + materialType.code + year) % 17 + 11))).forEach { file ->
                    addStored("Domain $domain/Material $materialType/$year/$file.ZIP")
                }
            }
        }

        (2018..2024).forEach { year ->
            (1..(4534 % ((domain.code + year) % 17 + 11))).forEach { file ->
                addStored("Domain $domain/Outputs/$year/$file.ZIP")
            }
        }
    }
}
