import 'dart:convert';
import 'package:flutter/material.dart';
import 'package:shared_preferences/shared_preferences.dart';
import 'package:movieappflutter/provider/movieprovider.dart';
import 'package:provider/provider.dart';
import 'package:url_launcher/url_launcher.dart';

class MovieDetails extends StatefulWidget {
  final String title;
  final String cover;
  final String director;
  final double rating;
  final String summary;
  final List<String> genres;
  final String releaseDate;

  const MovieDetails({
    Key? key,
    required this.title,
    required this.cover,
    required this.director,
    required this.rating,
    required this.summary,
    required this.genres,
    required this.releaseDate,
  }) : super(key: key);

  @override
  State<MovieDetails> createState() => _MovieDetailsState();
}

class _MovieDetailsState extends State<MovieDetails> {
  bool isFavorite = false;
  List<dynamic> relatedMovies = [];
  List<dynamic> trailers = [];

  @override
  void initState() {
    super.initState();
    _loadFavoriteStatus();
    _fetchRelatedMovies();
    _fetchTrailers();
  }

  Future<void> _loadFavoriteStatus() async {
    final prefs = await SharedPreferences.getInstance();
    setState(() {
      isFavorite = prefs.getBool(widget.title) ?? false;
    });
  }

  Future<void> _toggleFavorite() async {
    final prefs = await SharedPreferences.getInstance();
    setState(() {
      isFavorite = !isFavorite;
    });
    List<String> favorites = prefs.getStringList('favorites') ?? [];

    if (isFavorite) {
      if (!favorites.contains(widget.title)) {
        favorites.add(widget.title);
        await prefs.setString(
            widget.title,
            json.encode({
              "title": widget.title,
              "cover": widget.cover,
              "director": widget.director,
              "rating": widget.rating,
              "summary": widget.summary,
              "genres": widget.genres,
              "releaseDate": widget.releaseDate,
            }));
      }
    } else {
      favorites.remove(widget.title);
      prefs.remove(widget.title);
    }

    await prefs.setStringList('favorites', favorites);
  }

  int getGenreIdFromName(String genreName) {
    switch (genreName.toLowerCase()) {
      case 'action':
        return 28;
      case 'drama':
        return 18;
      case 'comedy':
        return 35;
      case 'adventure':
        return 12;
      case 'animation':
        return 16;
      default:
        return 0;
    }
  }

  Future<void> _fetchRelatedMovies() async {
    if (widget.genres.isNotEmpty) {
      final provider = Provider.of<MovieProvider>(context, listen: false);
      for (String genre in widget.genres) {
        int genreId = getGenreIdFromName(genre);
        if (genreId != 0) {
          final movies = await provider.fetchMoviesByGenreId(genreId);
          relatedMovies.addAll(movies);
        }
      }
      // إزالة التكرار
      relatedMovies = relatedMovies.toSet().toList();
      setState(() {});
    }
  }

  Future<void> _fetchTrailers() async {
    final provider = Provider.of<MovieProvider>(context, listen: false);
    trailers = await provider.fetchMovieTrailersByTitle(widget.title);
    setState(() {});
  }

  void _launchTrailer(String key) async {
    final url = 'https://www.youtube.com/watch?v=$key';
    if (await canLaunch(url)) {
      await launch(url);
    } else {
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(content: Text('Cannot open trailer')),
      );
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: Colors.black,
      appBar: AppBar(
        backgroundColor: Colors.black,
        iconTheme: const IconThemeData(color: Colors.white),
        title: Text(
          widget.title,
          style: const TextStyle(color: Colors.white),
        ),
      ),
      body: SingleChildScrollView(
        child: Padding(
          padding: const EdgeInsets.symmetric(vertical: 16, horizontal: 12),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Image.network(widget.cover, height: 300, fit: BoxFit.cover),
              const SizedBox(height: 16),
              Text('Director: ${widget.director}',
                  style: const TextStyle(color: Colors.white)),
              Text('Rating: ${widget.rating}',
                  style: const TextStyle(color: Colors.white)),
              Text('Release Date: ${widget.releaseDate}',
                  style: const TextStyle(color: Colors.white)),
              Text('Genres: ${widget.genres.join(', ')}',
                  style: const TextStyle(color: Colors.white)),
              const SizedBox(height: 16),

              // قصة الفيلم
              const Text('Summary',
                  style: TextStyle(color: Colors.white, fontSize: 18)),
              const SizedBox(height: 8),
              Text(widget.summary,
                  style: const TextStyle(color: Colors.white, fontSize: 14)),
              const SizedBox(height: 24),

              // عرض التريليرات
              if (trailers.isNotEmpty) ...[
                const Text('Trailers',
                    style: TextStyle(color: Colors.white, fontSize: 18)),
                const SizedBox(height: 8),
                SizedBox(
                  height: 140,
                  child: ListView.builder(
                    scrollDirection: Axis.horizontal,
                    itemCount: trailers.length,
                    itemBuilder: (context, index) {
                      final trailer = trailers[index];
                      return GestureDetector(
                        onTap: () => _launchTrailer(trailer['key']),
                        child: Container(
                          width: 250,
                          margin: const EdgeInsets.all(8),
                          decoration: BoxDecoration(
                            color: Colors.grey[900],
                            borderRadius: BorderRadius.circular(8),
                          ),
                          child: Center(
                            child: Column(
                              mainAxisAlignment: MainAxisAlignment.center,
                              children: [
                                const Icon(Icons.play_circle_fill,
                                    color: Colors.red, size: 50),
                                const SizedBox(height: 8),
                                Text(
                                  trailer['name'] ?? 'Trailer',
                                  style: const TextStyle(
                                      color: Colors.white, fontSize: 16),
                                  textAlign: TextAlign.center,
                                  maxLines: 2,
                                  overflow: TextOverflow.ellipsis,
                                ),
                              ],
                            ),
                          ),
                        ),
                      );
                    },
                  ),
                ),
                const SizedBox(height: 24),
              ],

              // الأفلام ذات نفس النوع مع إمكانية الضغط عليهم للانتقال لتفاصيل الفيلم
              if (relatedMovies.isNotEmpty) ...[
                const Text('Related Movies',
                    style: TextStyle(color: Colors.white, fontSize: 18)),
                const SizedBox(height: 8),
                SizedBox(
                  height: 220,
                  child: ListView.builder(
                    scrollDirection: Axis.horizontal,
                    itemCount: relatedMovies.length,
                    itemBuilder: (context, index) {
                      final movie = relatedMovies[index];
                      return GestureDetector(
                        onTap: () {
                          Navigator.push(
                            context,
                            MaterialPageRoute(
                              builder: (context) => MovieDetails(
                                title: movie['title'] ?? '',
                                cover:
                                    'https://image.tmdb.org/t/p/w500${movie['poster_path']}',
                                director: 'Unknown',
                                rating: (movie['vote_average'] ?? 0).toDouble(),
                                summary: movie['overview'] ?? '',
                                genres: [],
                                releaseDate: movie['release_date'] ?? '',
                              ),
                            ),
                          );
                        },
                        child: Padding(
                          padding: const EdgeInsets.all(8.0),
                          child: Column(
                            children: [
                              Image.network(
                                'https://image.tmdb.org/t/p/w500${movie['poster_path']}',
                                height: 170,
                                fit: BoxFit.cover,
                              ),
                              const SizedBox(height: 8),
                              SizedBox(
                                width: 100,
                                child: Text(
                                  movie['title'] ?? '',
                                  style: const TextStyle(color: Colors.white),
                                  textAlign: TextAlign.center,
                                  maxLines: 2,
                                  overflow: TextOverflow.ellipsis,
                                ),
                              ),
                            ],
                          ),
                        ),
                      );
                    },
                  ),
                ),
              ],
            ],
          ),
        ),
      ),
      floatingActionButton: FloatingActionButton(
        onPressed: _toggleFavorite,
        backgroundColor: isFavorite ? Colors.red : Colors.grey,
        child: Icon(isFavorite ? Icons.favorite : Icons.favorite_border),
      ),
    );
  }
}
