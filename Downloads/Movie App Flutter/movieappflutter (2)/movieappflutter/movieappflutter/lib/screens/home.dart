import 'dart:async';
import 'package:flutter/material.dart';
import 'package:movieappflutter/provider/movieprovider.dart';
import 'package:movieappflutter/screens/favoritepage.dart';
import 'package:movieappflutter/widgets/gener.dart';
import 'package:movieappflutter/widgets/header.dart';
import 'package:movieappflutter/screens/movie.dart';
import 'package:provider/provider.dart';

class HomeScreen extends StatefulWidget {
  const HomeScreen({super.key});

  @override
  State<HomeScreen> createState() => _HomeScreenState();
}

class _HomeScreenState extends State<HomeScreen> {
  String searchQuery = '';
  Timer? _debounce;

  @override
  Widget build(BuildContext context) {
    final movieProvider = Provider.of<MovieProvider>(context);
    movieProvider.fetchMovies();

    final allMovies = [
      ...movieProvider.actionMovies,
      ...movieProvider.dramaMovies,
      ...movieProvider.comedyMovies,
      ...movieProvider.adventureMovies,
      ...movieProvider.animationMovies,
    ];

    final filteredMovies = searchQuery.isEmpty
        ? movieProvider.searchResults
        : movieProvider.searchResults.where((movie) {
            final title = (movie['title'] ?? '').toLowerCase();
            return title.contains(searchQuery.toLowerCase());
          }).toList();

    return Scaffold(
      backgroundColor: Colors.black,
      appBar: null,
      body: SafeArea(
        child: CustomScrollView(
          slivers: [
            _buildSliverAppBar(),
            _buildSearchBar(movieProvider),
            _buildMovieList(filteredMovies, movieProvider),
            _buildGenreSections(movieProvider),
          ],
        ),
      ),
    );
  }

// SliverAppBar
  SliverAppBar _buildSliverAppBar() {
    return SliverAppBar(
      expandedHeight: 400.0,
      floating: false,
      pinned: true,
      backgroundColor: Colors.black,
      title: const Text(
        'Home',
        style: TextStyle(color: Colors.white),
      ),
      actions: [
        IconButton(
          icon: const Icon(Icons.favorite, color: Colors.red),
          onPressed: () {
            Navigator.push(
              context,
              MaterialPageRoute(builder: (context) => const FavoritesScreen()),
            );
          },
        ),
      ],
      flexibleSpace: const FlexibleSpaceBar(
        background: Header(),
      ),
    );
  }

// Search Bar with Debounce Logic
  SliverToBoxAdapter _buildSearchBar(MovieProvider movieProvider) {
    return SliverToBoxAdapter(
      child: Padding(
        padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 8),
        child: TextField(
          style: const TextStyle(color: Colors.white),
          decoration: InputDecoration(
            hintText: 'Search movies...',
            hintStyle: const TextStyle(color: Colors.white54),
            filled: true,
            fillColor: Colors.grey[900],
            prefixIcon: const Icon(Icons.search, color: Colors.white),
            border: OutlineInputBorder(
              borderRadius: BorderRadius.circular(10),
              borderSide: BorderSide.none,
            ),
          ),
          onChanged: (value) {
            if (_debounce?.isActive ?? false) _debounce!.cancel();
            _debounce = Timer(const Duration(milliseconds: 500), () {
              setState(() {
                searchQuery = value;
              });

              if (value.isNotEmpty) {
                movieProvider.searchMovies(value);
              } else {
                movieProvider.clearSearchResults();
              }
            });
          },
        ),
      ),
    );
  }

// Movie List
  SliverList _buildMovieList(List filteredMovies, MovieProvider movieProvider) {
    return SliverList(
      delegate: SliverChildBuilderDelegate(
        (context, index) {
          final movie = filteredMovies[index];
          final posterPath = movie['poster_path'];
          final imageUrl = posterPath != null
              ? 'https://image.tmdb.org/t/p/w500$posterPath'
              : 'https://via.placeholder.com/100x150?text=No+Image';

          return ListTile(
            leading: Image.network(imageUrl, width: 50),
            title: Text(
              movie['title'] ?? 'No Title',
              style: const TextStyle(color: Colors.white),
            ),
            subtitle: Text(
              movie['overview'] ?? '',
              maxLines: 2,
              overflow: TextOverflow.ellipsis,
              style: const TextStyle(color: Colors.white70),
            ),
            onTap: () async {
              try {
                final movieDetails =
                    await movieProvider.fetchMovieDetails(movie['id']);
                Navigator.push(
                  context,
                  MaterialPageRoute(
                    builder: (_) => MovieDetails(
                      title: movieDetails['title'] ?? 'No Title',
                      cover: movieDetails['poster_path'] != null
                          ? 'https://image.tmdb.org/t/p/w500${movieDetails['poster_path']}'
                          : 'https://via.placeholder.com/100x150?text=No+Image',
                      director: movieDetails['director'] ?? 'Unknown',
                      rating: movieDetails['vote_average'] ?? 0.0,
                      summary: movieDetails['overview'] ?? 'No description.',
                      genres: (movieDetails['genres'] as List)
                          .map((genre) => genre['name'].toString())
                          .toList(),
                      releaseDate: movieDetails['release_date'] ?? 'N/A',
                    ),
                  ),
                );
              } catch (e) {
                print("Error navigating to MovieDetails: $e");
              }
            },
          );
        },
        childCount: filteredMovies.length,
      ),
    );
  }

// Genre Sections
  SliverList _buildGenreSections(MovieProvider movieProvider) {
    return SliverList(
      delegate: SliverChildListDelegate(
        [
          Gener(title: 'Action', movies: movieProvider.actionMovies),
          Gener(title: 'Drama', movies: movieProvider.dramaMovies),
          Gener(title: 'Comedy', movies: movieProvider.comedyMovies),
          Gener(title: 'Adventure', movies: movieProvider.adventureMovies),
          Gener(title: 'Animation', movies: movieProvider.animationMovies),
        ],
      ),
    );
  }
}