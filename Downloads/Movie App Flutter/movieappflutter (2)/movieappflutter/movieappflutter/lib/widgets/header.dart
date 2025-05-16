import 'dart:convert';
import 'dart:math';
import 'package:flutter/material.dart';
import 'package:http/http.dart' as http show get;
import 'package:movieappflutter/screens/movie.dart';
import 'package:movieappflutter/provider/movieprovider.dart';
import 'package:provider/provider.dart';

const String apiKey = '58e89d4f4a17f51f45d0ea77655c3546';
const String baseUrl = 'https://api.themoviedb.org/3';

class Header extends StatefulWidget {
  const Header({super.key});

  @override
  State<Header> createState() => _HeaderState();
}

class _HeaderState extends State<Header> {
  String title = '';
  String coverUrl = '';
  double rating = 0.0;
  int year = 0;
  String overview = '';
  bool isLoading = true;
  int movieId = 0; // Add movie ID to fetch movie details

  @override
  void initState() {
    super.initState();
    getForYouMovie();
  }

  Future<void> getForYouMovie() async {
    Random random = Random();
    int randomPage = random.nextInt(10) + 1;

    final url = '$baseUrl/discover/movie?api_key=$apiKey&page=$randomPage';
    final response = await http.get(Uri.parse(url));

    if (response.statusCode == 200) {
      final data = json.decode(response.body);
      final results = data['results'];

      if (results != null && results.isNotEmpty) {
        int randomIndex = random.nextInt(results.length);
        final movie = results[randomIndex];

        setState(() {
          title = movie['title'] ?? 'No Title';
          coverUrl = 'https://image.tmdb.org/t/p/w500${movie['backdrop_path']}';
          rating = (movie['vote_average'] ?? 0).toDouble();
          year = DateTime.tryParse(movie['release_date'] ?? '')?.year ?? 0;
          overview = movie['overview'] ?? '';
          movieId = movie['id']; // Store movie ID
          isLoading = false;
        });
      }
    } else {
      throw Exception('Failed to load movie');
    }
  }

  @override
  Widget build(BuildContext context) {
    return GestureDetector(
      onTap: () async {
        // Fetch detailed movie data using the movie ID
        final movieProvider =
            Provider.of<MovieProvider>(context, listen: false);
        final movieDetails =
            await movieProvider.fetchMovieDetails(movieId); // Pass the movie ID

        // Handle runtime

        // Extract genres and cast
        final genres = (movieDetails['genres'] as List<dynamic>)
            .map((genre) => genre['name'].toString())
            .toList();

        // Extract director, release date, and streaming provider
        final director = (movieDetails['credits']['crew'] as List<dynamic>)
            .firstWhere((crew) => crew['job'] == 'Director',
                orElse: () => {'name': 'Unknown'})['name'];
        final releaseDate = movieDetails['release_date'] ?? 'N/A';

        // Navigate to MovieDetails screen with the detailed data
        Navigator.push(
          context,
          MaterialPageRoute(
            builder: (_) => MovieDetails(
              title: movieDetails['title'] ?? 'No Title',
              cover: coverUrl,
              director: director,
              rating: (movieDetails['vote_average'] ?? 0).toDouble(),
              summary: movieDetails['overview'] ?? 'No description.',
              genres: genres,
              releaseDate: releaseDate,
            ),
          ),
        );
      },
      child: Stack(
        children: [
          Container(
            width: double.infinity,
            height: 400,
            decoration: const BoxDecoration(color: Colors.black),
            child: isLoading
                ? const Center(child: CircularProgressIndicator())
                : Stack(
                    fit: StackFit.expand,
                    children: [
                      Image.network(coverUrl, fit: BoxFit.cover),
                      Container(
                        decoration: BoxDecoration(
                          gradient: LinearGradient(
                            colors: [
                              Colors.transparent,
                              Colors.black.withOpacity(0.7),
                            ],
                            begin: Alignment.topCenter,
                            end: Alignment.bottomCenter,
                          ),
                        ),
                      ),
                      Positioned(
                        left: 16,
                        bottom: 30,
                        right: 16,
                        child: Column(
                          crossAxisAlignment: CrossAxisAlignment.start,
                          children: [
                            Text(
                              title,
                              style: const TextStyle(
                                color: Colors.white,
                                fontSize: 24,
                                fontWeight: FontWeight.bold,
                              ),
                            ),
                            const SizedBox(height: 8),
                            Text(
                              '$year | Rating: $rating',
                              style: const TextStyle(color: Colors.white70),
                            ),
                            const SizedBox(height: 8),
                            Text(
                              overview,
                              maxLines: 3,
                              overflow: TextOverflow.ellipsis,
                              style: const TextStyle(color: Colors.white),
                            ),
                          ],
                        ),
                      ),
                    ],
                  ),
          ),
        ],
      ),
    );
  }
}
