package com.flixclusive.data.configuration.di.test.constant

internal const val SEARCH_CATEGORIES = """
{
    "networks": [
        {
            "id": 56,
            "type": "tv",
            "name": "Cartoon Network",
            "poster_path": "/c5OC6oVCg6QP4eqzW6XIq17CQjI.png",
            "query": "discover/tv?include_adult=false&include_video=false&language=en-US&sort_by=popularity.desc&first_air_date.gte=1990-01-01&with_networks=56"
        }
    ],
    "companies": [
        {
            "id": 1632,
            "type": "movie",
            "name": "Lionsgate",
            "poster_path": "/cisLn1YAUuptXVBa0xjq7ST9cH0.png",
            "query": "discover/movie?include_adult=false&include_video=false&language=en-US&sort_by=popularity.desc&primary_release_date.gte=1990-01-01&with_companies=35|1632"
        }
    ],
    "genres": [
        {
            "id": 28,
            "type": "movie",
            "name": "Action",
            "query": "discover/movie?include_adult=false&include_video=false&language=en-US&sort_by=popularity.desc&primary_release_date.gte=1990-01-01&with_genres=28"
        }
    ],
    "type": [
        {
            "name": "TV Shows",
            "id": -1,
            "type": "tv",
            "query": "discover/tv?include_adult=false&include_video=false&language=en-US&sort_by=popularity.desc&first_air_date.gte=1990-01-01&without_genres=10763,10767"
        }
    ]
}
"""
