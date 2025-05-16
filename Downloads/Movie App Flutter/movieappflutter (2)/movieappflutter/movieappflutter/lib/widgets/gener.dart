import 'package:flutter/material.dart';
import 'package:movieappflutter/screens/movie.dart';
import 'package:movieappflutter/provider/movieprovider.dart';
import 'package:provider/provider.dart';

class Gener extends StatelessWidget {
  final String title;
  final List movies;

  const Gener({super.key, required this.title, required this.movies});

  @override
  Widget build(BuildContext context) {
    return Padding(
      padding: const EdgeInsets.only(top: 10, left: 10),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Text(
            title,
            style: const TextStyle(fontSize: 20, color: Colors.white),
          ),
          const SizedBox(height: 5),
          SizedBox(
            height: 180,
            child: ListView.builder(
              scrollDirection: Axis.horizontal,
              itemCount: movies.length,
              itemBuilder: (context, index) {
                final movie = movies[index];
                final posterPath = movie['poster_path'];
                final imageUrl = posterPath != null
                    ? 'https://image.tmdb.org/t/p/w500$posterPath'
                    : 'https://via.placeholder.com/100x150?text=No+Image';

                return Padding(
                  padding: const EdgeInsets.only(right: 10),
                  child: GestureDetector(
                    onTap: () async {
                      final movieProvider =
                          Provider.of<MovieProvider>(context, listen: false);

                      // Fetch full movie details
                      final movieDetails =
                          await movieProvider.fetchMovieDetails(movie['id']);

                      // Fetch trailer key
                      final trailers =
                          await movieProvider.fetchMovieTrailers(movie['id']);
                      final youtubeTrailer = trailers.firstWhere(
                        (video) =>
                            video['site'] == 'YouTube' &&
                            video['type'] == 'Trailer',
                        orElse: () => null,
                      );
                      final videoKey =
                          youtubeTrailer != null ? youtubeTrailer['key'] : '';

                      final runtime = (movieDetails['runtime'] is int)
                          ? movieDetails['runtime'].toString()
                          : 'N/A';

                      final genres = (movieDetails['genres'] as List<dynamic>)
                          .map((genre) => genre['name'].toString())
                          .toList();

                      final director =
                          (movieDetails['credits']['crew'] as List<dynamic>)
                              .firstWhere((crew) => crew['job'] == 'Director',
                                  orElse: () => {'name': 'Unknown'})['name'];

                      final releaseDate = movieDetails['release_date'] ?? 'N/A';

                      Navigator.push(
                        context,
                        MaterialPageRoute(
                          builder: (_) => MovieDetails(
                            title: movieDetails['title'] ?? 'No Title',
                            cover: imageUrl,
                            director: director,
                            rating:
                                (movieDetails['vote_average'] ?? 0).toDouble(),
                            summary:
                                movieDetails['overview'] ?? 'No description.',
                            genres: genres,
                            releaseDate: releaseDate,
                          ),
                        ),
                      );
                    },
                    child: Container(
                      width: 120,
                      decoration: BoxDecoration(
                        borderRadius: BorderRadius.circular(10),
                        color: Colors.grey[900],
                        image: DecorationImage(
                          image: NetworkImage(imageUrl),
                          fit: BoxFit.cover,
                        ),
                      ),
                    ),
                  ),
                );
              },
            ),
          ),
        ],
      ),
    );
  }
}
