package com.flixclusive.data.configuration.di.test.constant

internal const val HOME_CATEGORIES = """
{
    "all": [
        {
           "name": "Trending Now",
           "type": "all",
           "required": true,
           "canPaginate": true,
           "query": "trending/all/day?language=en-US"
        }
    ],
    "movie": [
        {
           "name": "Top Movies Recently",
           "type": "movie",
           "required": true,
           "canPaginate": true,
           "query": "discover/movie?include_adult=false&include_video=false&language=en-US&sort_by=popularity.desc&primary_release_date.gte=1990-01-01"
        }
    ],
    "tv": [
        {
           "name": "Top TV Shows Recently",
           "type": "tv",
           "required": true,
           "canPaginate": true,
           "query": "discover/tv?include_adult=false&include_video=false&language=en-US&sort_by=popularity.desc&without_genres=10763,10767"
        }
    ]
}
"""
