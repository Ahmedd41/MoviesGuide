import 'package:flutter/material.dart';
import 'package:http/http.dart' as http;
import 'dart:convert';

const String apiKey = '92b13daaddc3217174df062a1f6c8e51';
const String baseUrl = 'https://api.themoviedb.org/3';

class MovieProvider extends ChangeNotifier {
// Movie Lists
  List<dynamic> _actionMovies = [];
  List<dynamic> _dramaMovies = [];
  List<dynamic> _comedyMovies = [];
  List<dynamic> _adventureMovies = [];
  List<dynamic> _animationMovies = [];
  List<dynamic> _searchResults = [];

// Getters
  List<dynamic> get actionMovies => _actionMovies;
  List<dynamic> get dramaMovies => _dramaMovies;
  List<dynamic> get comedyMovies => _comedyMovies;
  List<dynamic> get adventureMovies => _adventureMovies;
  List<dynamic> get animationMovies => _animationMovies;
  List<dynamic> get searchResults => _searchResults;

  bool _isLoading = false;
  bool get isLoading => _isLoading;

// Fetch all movies by genres
  Future<void> fetchMovies() async {
    _isLoading = true;
    notifyListeners();
    try {
      await Future.wait([
        _fetchGenreMovies(28, (movies) => _actionMovies = movies),
        _fetchGenreMovies(18, (movies) => _dramaMovies = movies),
        _fetchGenreMovies(35, (movies) => _comedyMovies = movies),
        _fetchGenreMovies(12, (movies) => _adventureMovies = movies),
        _fetchGenreMovies(16, (movies) => _animationMovies = movies),
      ]);
    } catch (error) {
      debugPrint('Error fetching movies: $error');
    } finally {
      _isLoading = false;
      notifyListeners();
    }
  }

// Unified fetch method
  Future<void> _fetchGenreMovies(
      int genreId, Function(List<dynamic>) setMovies) async {
    final url = '$baseUrl/discover/movie?api_key=$apiKey&with_genres=$genreId';
    final response = await _fetchData(url);
    if (response != null) {
      setMovies(response['results']);
    }
  }

  Future<List<dynamic>> fetchMovieTrailersByTitle(String title) async {
    try {
      final searchUrl = Uri.parse(
        'https://api.themoviedb.org/3/search/movie?api_key=$apiKey&query=$title',
      );
      final searchData = json.decode((await http.get(searchUrl)).body);

      final movieId = searchData['results']?.first['id'];
      if (movieId == null) return [];

      final trailersUrl = Uri.parse(
        'https://api.themoviedb.org/3/movie/$movieId/videos?api_key=$apiKey',
      );
      final trailersData = json.decode((await http.get(trailersUrl)).body);

      return (trailersData['results'] as List)
          .where((video) => video['type'] == 'Trailer')
          .toList();
    } catch (e) {
      print('Error fetching trailers: $e');
      return [];
    }
  }

  Future<List<dynamic>> fetchMoviesByGenreId(int genreId) async {
    final url = '$baseUrl/discover/movie?api_key=$apiKey&with_genres=$genreId';
    final response = await _fetchData(url);
    if (response != null) {
      return response['results'];
    } else {
      return [];
    }
  }

// Fetch movie details
  Future<Map<String, dynamic>> fetchMovieDetails(int movieId) async {
    final url =
        '$baseUrl/movie/$movieId?api_key=$apiKey&append_to_response=credits,watch/providers';
    final response = await _fetchData(url);
    return response ?? {};
  }

// Fetch trailers for a movie
  Future<List<dynamic>> fetchMovieTrailers(int movieId) async {
    final url = '$baseUrl/movie/$movieId/videos?api_key=$apiKey';
    final response = await _fetchData(url);
    return response?['results'] ?? [];
  }

// Search movies
  Future<void> searchMovies(String query) async {
    _isLoading = true;
    notifyListeners();

    final url = '$baseUrl/search/movie?api_key=$apiKey&query=$query';
    final response = await _fetchData(url);
    if (response != null) {
      _searchResults = response['results'];
    }

    _isLoading = false;
    notifyListeners();
  }

  void clearSearchResults() {
    _searchResults.clear();
    notifyListeners();
  }

// Generic method to fetch data from API
  Future<Map<String, dynamic>?> _fetchData(String url) async {
    try {
      final response = await http.get(Uri.parse(url));
      if (response.statusCode == 200) {
        return json.decode(response.body);
      } else {
        debugPrint('Failed to fetch data from $url');
        return null;
      }
    } catch (e) {
      debugPrint('Error: $e');
      return null;
    }
  }

  Future<List<dynamic>> fetchSimilarMovies(int movieId) async {
    final response = await http
        .get(Uri.parse('$baseUrl/movie/$movieId/similar?api_key=$apiKey'));

    if (response.statusCode == 200) {
      final data = json.decode(response.body);
      return data['results']; // إرجاع قائمة الأفلام المشابهة
    } else {
      throw Exception('Failed to load similar movies');
    }
  }
}
