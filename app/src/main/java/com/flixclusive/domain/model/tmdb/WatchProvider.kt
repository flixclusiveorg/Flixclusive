package com.flixclusive.domain.model.tmdb

import androidx.annotation.StringRes
import com.flixclusive.R

enum class WatchProvider(
    val id: Int,
    @StringRes val labelId: Int,
    val providerName: String,
    val posterPath: String,
    val isCompany: Boolean
) {
    Marvel(
        id = 420,
        labelId = R.string.marvel_studios,
        posterPath = "/hUzeosd33nzE5MCNsZxCGEKTXaQ.png",
        isCompany = true,
        providerName = "Marvel Studios"
    ),
    Sony(
        id = 2251,
        labelId = R.string.sony_pictures,
        posterPath = "/5ilV5mH3gxTEU7p5wjxptHvXkyr.png",
        isCompany = true,
        providerName = "Sony Pictures"
    ),
    Pixar(
        id = 3,
        labelId = R.string.pixar,
        posterPath = "/1TjvGVDMYsj6JBxOAkUHpPEwLf7.png",
        isCompany = true,
        providerName = "Pixar"
    ),
    WaltDisney(
        id = 2,
        labelId = R.string.walt_disney_pictures,
        posterPath = "/wdrCwmRnLFJhEoH8GSfymY85KHT.png",
        isCompany = true,
        providerName = "Walt Disney Pictures"
    ),
    WarnerBros(
        id = 174,
        labelId = R.string.warner_brothers_pictures,
        posterPath = "/IuAlhI9eVC9Z8UQWOIDdWRKSEJ.png",
        isCompany = true,
        providerName = "Warner Bros."
    ),
    Paramount(
        id = 4,
        labelId = R.string.paramount_pictures,
        posterPath = "/gz66EfNoYPqHTYI4q9UEN4CbHRc.png",
        isCompany = true,
        providerName = "Paramount Pictures"
    ),
    Dreamworks(
        id = 7,
        labelId = R.string.dreamworks_pictures,
        posterPath = "/vru2SssLX3FPhnKZGtYw00pVIS9.png",
        isCompany = true,
        providerName = "Dreamwork Pictures"
    ),
    Columbia(
        id = 5,
        labelId = R.string.columbia_pictures,
        posterPath = "/71BqEFAF4V3qjjMPCpLuyJFB9A.png",
        isCompany = true,
        providerName = "Columbia Pictures"
    ),
    Netflix(
        id = 213,
        labelId = R.string.netflix,
        posterPath = "/wwemzKWzjKYJFfCeiB57q3r4Bcm.png",
        isCompany = false,
        providerName = "Netflix"
    ),
    Hbo(
        id = 49,
        labelId = R.string.hbo,
        posterPath = "/tuomPhY2UtuPTqqFnKMVHvSb724.png",
        isCompany = false,
        providerName = "HBO"
    ),
    Amazon(
        id = 1024,
        labelId = R.string.amazon,
        posterPath = "/ifhbNuuVnlwYy5oXA5VIb2YR8AZ.png",
        isCompany = false,
        providerName = "Amazon"
    ),
    DisneyPlus(
        id = 2739,
        labelId = R.string.disney_plus,
        posterPath = "/uzKjVDmQ1WRMvGBb7UNRE0wTn1H.png",
        isCompany = false,
        providerName = "Disney+"
    ),
    AppleTv(
        id = 2552,
        labelId = R.string.apple_tv,
        posterPath = "/4KAy34EHvRM25Ih8wb82AuGU7zJ.png",
        isCompany = false,
        providerName = "Apple TV"
    ),
    CW(
        id = 71,
        labelId = R.string.the_cw,
        posterPath = "/ge9hzeaU7nMtQ4PjkFlc68dGAJ9.png",
        isCompany = false,
        providerName = "The CW"
    );
}