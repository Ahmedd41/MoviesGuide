import 'dart:convert';
import 'package:flutter/material.dart';
import 'package:movieappflutter/screens/movie.dart';
import 'package:shared_preferences/shared_preferences.dart';

class FavoritesScreen extends StatefulWidget {
  const FavoritesScreen({Key? key}) : super(key: key);

  @override
  _FavoritesScreenState createState() => _FavoritesScreenState();
}

Future<List<Map<String, dynamic>>> getFavoriteMovies() async {
  final prefs = await SharedPreferences.getInstance();

  List<String> favoriteTitles = prefs.getStringList('favorites') ?? [];
  List<Map<String, dynamic>> favoriteMovies = [];

  for (String title in favoriteTitles) {
    String? movieData = prefs.getString(title);
    if (movieData != null) {
      favoriteMovies.add(Map<String, dynamic>.from(json.decode(movieData)));
    }
  }

  return favoriteMovies;
}

class _FavoritesScreenState extends State<FavoritesScreen> {
  List<Map<String, dynamic>> favoriteMovies = [];

  @override
  void initState() {
    super.initState();
    _loadFavorites();
  }

  Future<void> _loadFavorites() async {
    final movies = await getFavoriteMovies();
    setState(() {
      favoriteMovies = movies;
    });
  }

  Future<void> _removeFromFavorites(String title) async {
    final prefs = await SharedPreferences.getInstance();
    List<String> favoriteTitles = prefs.getStringList('favorites') ?? [];

    if (favoriteTitles.contains(title)) {
      favoriteTitles.remove(title);
      await prefs.setStringList('favorites', favoriteTitles);
      await prefs.remove(title);

      // تحديث الواجهة بعد الحذف
      setState(() {
        favoriteMovies.removeWhere((movie) => movie['title'] == title);
      });
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: Colors.black,
      appBar: AppBar(
        backgroundColor: Colors.black,
        iconTheme: const IconThemeData(color: Colors.white),
        title: const Text(
          "Favorites",
          style: TextStyle(color: Colors.white),
        ),
      ),
      body: favoriteMovies.isEmpty
          ? const Center(
              child: Text(
                "No favorites yet!",
                style: TextStyle(color: Colors.white, fontSize: 18),
              ),
            )
          : ListView.builder(
              itemCount: favoriteMovies.length,
              itemBuilder: (context, index) {
                final movie = favoriteMovies[index];
                return Card(
                  color: Colors.black87,
                  margin:
                      const EdgeInsets.symmetric(vertical: 8, horizontal: 16),
                  child: ListTile(
                    leading: Image.network(movie['cover']),
                    title: Text(
                      movie['title'],
                      style: const TextStyle(color: Colors.white),
                    ),
                    subtitle: Text(
                      "Rating: ${movie['rating']} | Year: ${movie['year']}",
                      style: const TextStyle(color: Colors.white70),
                    ),
                    trailing: IconButton(
                      icon: const Icon(Icons.delete, color: Colors.red),
                      onPressed: () async {
                        await _removeFromFavorites(movie['title']);
                      },
                    ),
                    onTap: () {
                      Navigator.push(
                        context,
                        MaterialPageRoute(
                          builder: (context) => MovieDetails(
                            title: movie['title'],
                            cover: movie['cover'],
                            director: movie['director'],
                            rating: movie['rating'],
                            summary: movie['summary'],
                            genres: List<String>.from(movie['genres']),
                            releaseDate: movie['releaseDate'],
                          ),
                        ),
                      );
                    },
                  ),
                );
              },
            ),
    );
  }
}
