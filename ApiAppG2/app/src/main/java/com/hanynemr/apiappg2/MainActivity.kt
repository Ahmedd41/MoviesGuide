package com.hanynemr.apiappg2

import android.os.Bundle
import android.view.View
import android.widget.*
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.android.volley.RequestQueue
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import org.json.JSONObject
import android.content.Intent
import android.net.Uri
import android.content.ActivityNotFoundException
import com.bumptech.glide.Glide
import android.graphics.Color
import androidx.core.app.NotificationCompat
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import androidx.core.app.NotificationManagerCompat
import android.Manifest
import android.content.pm.PackageManager
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import android.app.PendingIntent
import org.json.JSONArray
import kotlin.random.Random
import android.widget.*
import android.widget.ArrayAdapter
import android.widget.Spinner
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.*
import java.util.Calendar


class MainActivity : AppCompatActivity() {
    lateinit var movieText: EditText
    lateinit var resultText: TextView
    lateinit var trailerButton: Button
    lateinit var favoriteButton: Button
    private var trailerVideoId: String = ""
    lateinit var queue: RequestQueue
    lateinit var posterImage: ImageView
    lateinit var ratingText: TextView
    lateinit var imagesContainer: LinearLayout
    lateinit var reviewsText: TextView
    lateinit var genreSpinner: Spinner
    lateinit var yearSpinner: Spinner
    lateinit var favoritesButton: Button

    // Store current movie details
    private var currentMovieId: Int = -1
    private var currentMovieTitle: String = ""
    private var currentMoviePoster: String = ""

    // Notification channel IDs
    private val SEARCH_CHANNEL_ID = "search_channel"
    private val SUGGESTION_CHANNEL_ID = "suggestion_channel"


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        initializeViews()

        setupListeners()

        setupSpinners()

        requestNotificationPermission()

        lifecycleScope.launch {
            createNotificationChannels()

            // تهيئة طابور الطلبات بشكل متأخر
            initializeRequestQueue()

            // تأخير عرض إشعار بفيلم عشوائي بعد تحميل الواجهة
            delay(500) // تأخير بسيط للسماح للواجهة بالتحميل أولاً
            showRandomMovieSuggestion()
        }
    }

    // فصل عملية تعريف المكونات في دالة منفصلة
    private fun initializeViews() {
        movieText = findViewById(R.id.movieText)
        resultText = findViewById(R.id.resultText)
        trailerButton = findViewById(R.id.trailerButton)
        favoriteButton = findViewById(R.id.favoriteButton)
        trailerButton.isEnabled = false
        posterImage = findViewById(R.id.posterImage)
        ratingText = findViewById(R.id.ratingText)
        imagesContainer = findViewById(R.id.imagesContainer)
        reviewsText = findViewById(R.id.reviewsText)
        favoritesButton = findViewById(R.id.favoritesButton)
        genreSpinner = findViewById(R.id.genreSpinner)
        yearSpinner = findViewById(R.id.yearSpinner)
    }

    // تهيئة المستمعين للأحداث في دالة منفصلة
    private fun setupListeners() {
        trailerButton.setOnClickListener {
            if (trailerVideoId.isNotEmpty()) {
                playTrailerWithId(trailerVideoId)
            } else {
                Toast.makeText(this, "No trailer available", Toast.LENGTH_SHORT).show()
            }
        }

        favoriteButton.setOnClickListener {
            saveMovieAsFavorite()
        }

        favoritesButton.setOnClickListener {
            showFavoriteMovies()
        }
    }

    // تهيئة القوائم المنسدلة بطريقة أكثر كفاءة
    private fun setupSpinners() {
        // إنشاء قائمة الأنواع مسبقاً
        val genreMap = mapOf(
            "All" to null,
            "Action" to 28,
            "Comedy" to 35,
            "Drama" to 18,
            "Horror" to 27,
            "Romance" to 10749
        )
        setupGenreSpinner(genreMap)

        // إنشاء قائمة السنوات
        setupYearSpinner()
    }

    private fun setupGenreSpinner(genres: Map<String, Int?>) {
        val genreList = genres.keys.toList()
        val genreAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, genreList).apply {
            setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        }
        genreSpinner.adapter = genreAdapter
    }

    private fun setupYearSpinner() {
        val currentYear = Calendar.getInstance().get(Calendar.YEAR)
        val years = ArrayList<String>(10).apply {
            add("All")
            // استخدام حجم مسبق للقائمة وتعبئتها مرة واحدة
            for (i in 0..9) {
                add((currentYear - i).toString())
            }
        }
        val yearAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, years).apply {
            setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        }
        yearSpinner.adapter = yearAdapter
    }

    // تهيئة طابور الطلبات بشكل متأخر
    private fun initializeRequestQueue() {
        if (!::queue.isInitialized) {
            queue = Volley.newRequestQueue(applicationContext)
        }
    }
    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // قناة إشعارات البحث
            val searchChannel = NotificationChannel(
                SEARCH_CHANNEL_ID,
                "Search Notifications",
                NotificationManager.IMPORTANCE_DEFAULT
            )

            // قناة إشعارات اقتراحات الأفلام
            val suggestionChannel = NotificationChannel(
                SUGGESTION_CHANNEL_ID,
                "Movie Suggestions",
                NotificationManager.IMPORTANCE_DEFAULT
            )

            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(searchChannel)
            manager.createNotificationChannel(suggestionChannel)
        }
    }

    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                    1001
                )
            }
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 1001) {
            if ((grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                Toast.makeText(this, "Permission granted", Toast.LENGTH_SHORT).show()
                showRandomMovieSuggestion()
            } else {
                Toast.makeText(this, "Notification permission denied", Toast.LENGTH_SHORT).show()
            }
        }
    }

    fun show(view: View) {
        val movieQuery = movieText.text.toString().trim()
        val selectedGenreName = genreSpinner.selectedItem.toString()
        val selectedGenreId = when (selectedGenreName) {
            "All" -> null
            else -> mapOf(
                "Action" to 28,
                "Comedy" to 35,
                "Drama" to 18,
                "Horror" to 27,
                "Romance" to 10749
            )[selectedGenreName]
        }
        val selectedYear = yearSpinner.selectedItem.toString()

        var url: String

        // إرسال إشعار عن البحث
        sendSearchNotification(movieQuery)

        // 🟢 في حالة وجود اسم فيلم
        if (movieQuery.isNotEmpty()) {
            // تسجيل النوع إذا تم اختياره
            if (selectedGenreName != "All") {
                incrementGenreCount(selectedGenreName)
            }

            url = "https://api.themoviedb.org/3/search/movie?api_key=2ed7b6b728c2ff496be456864d2896dd&query=$movieQuery"
            if (selectedYear != "All") {
                url += "&primary_release_year=$selectedYear"
            }

            val request = JsonObjectRequest(url, { response ->
                try {
                    val results = response.getJSONArray("results")
                    if (results.length() > 0) {
                        val movie = results.getJSONObject(0)
                        val movieId = movie.getInt("id")

                        // تخزين اسم الفيلم للإشعارات المستقبلية
                        val movieTitle = movie.optString("title", "")
                        val posterPath = movie.optString("poster_path", "")
                        if (movieTitle.isNotEmpty()) {
                            saveSearchedMovie(movieId, movieTitle, posterPath, selectedGenreName)
                        }

                        fetchMovieDetails(movieId)
                    } else {
                        resultText.text = "No movies found."
                        resetMovieDetails()
                    }
                } catch (e: Exception) {
                    Toast.makeText(this, "Error processing search result", Toast.LENGTH_SHORT).show()
                }
            }, { onErrorResponse(it) })
            queue.add(request)
        }
        // 🔵 في حالة استخدام فلتر فقط (discover)
        else {
            url = "https://api.themoviedb.org/3/discover/movie?api_key=2ed7b6b728c2ff496be456864d2896dd&language=en-US"
            if (selectedGenreId != null) {
                url += "&with_genres=$selectedGenreId"
                incrementGenreCount(selectedGenreName)
            }
            if (selectedYear != "All") {
                url += "&primary_release_year=$selectedYear"
            }

            val request = JsonObjectRequest(url, { response ->
                try {
                    val results = response.getJSONArray("results")

                    imagesContainer.removeAllViews()
                    resultText.text = ""
                    resetMovieDetails()

                    if (results.length() > 0) {
                        for (i in 0 until results.length()) {
                            val movie = results.getJSONObject(i)
                            val title = movie.optString("title", "No Title")
                            val overview = movie.optString("overview", "No overview")
                            val rating = movie.optDouble("vote_average", 0.0)
                            val posterPath = movie.optString("poster_path", "")
                            val movieId = movie.getInt("id")

                            // تخزين الأفلام المكتشفة للإشعارات المستقبلية
                            if (title.isNotEmpty()) {
                                saveSearchedMovie(movieId, title, posterPath, selectedGenreName)
                            }

                            // 📝 عرض النصوص: الاسم + النبذة + التقييم
                            val infoText = TextView(this)
                            infoText.text = "🎬 $title\n📖 $overview\n⭐ Rating: $rating\n"
                            infoText.setPadding(0, 20, 0, 10)
                            infoText.textSize = 16f
                            imagesContainer.addView(infoText)

                            if (posterPath.isNotEmpty()) {
                                val imageUrl = "https://image.tmdb.org/t/p/w500$posterPath"
                                val imageView = ImageView(this)
                                imageView.layoutParams = LinearLayout.LayoutParams(500, 700)
                                imageView.setPadding(0, 0, 0, 30)
                                imageView.scaleType = ImageView.ScaleType.CENTER_CROP
                                Glide.with(this).load(imageUrl).into(imageView)
                                imagesContainer.addView(imageView)
                            }

                            // Add a View Details button for this movie
                            val detailsButton = Button(this)
                            detailsButton.text = "View Details"
                            detailsButton.setOnClickListener {
                                fetchMovieDetails(movieId)
                            }
                            imagesContainer.addView(detailsButton)

                            // 🔻 خط فاصل بين كل فيلم والتاني
                            val divider = View(this)
                            divider.layoutParams = LinearLayout.LayoutParams(
                                LinearLayout.LayoutParams.MATCH_PARENT, 4
                            )
                            divider.setBackgroundColor(Color.LTGRAY)
                            imagesContainer.addView(divider)
                        }
                    } else {
                        resultText.text = "No movies found."
                    }

                } catch (e: Exception) {
                    Toast.makeText(this, "Error processing discover result", Toast.LENGTH_SHORT).show()
                }
            }, { onErrorResponse(it) })
            queue.add(request)
        }
    }

    private fun sendSearchNotification(query: String) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                == PackageManager.PERMISSION_GRANTED) {

                val builder = NotificationCompat.Builder(this, SEARCH_CHANNEL_ID)
                    .setSmallIcon(R.drawable.ic_launcher_foreground)
                    .setContentTitle("Movie Search")
                    .setContentText("You searched for: ${if (query.isNotEmpty()) query else "movies with filters"}")
                    .setPriority(NotificationCompat.PRIORITY_DEFAULT)

                with(NotificationManagerCompat.from(this)) {
                    notify(1002, builder.build())
                }
            }
        }
    }

    // تخزين الأفلام التي تم البحث عنها
    private fun saveSearchedMovie(id: Int, title: String, posterPath: String, genre: String) {
        val prefs = getSharedPreferences("searched_movies", MODE_PRIVATE)
        val moviesJson = prefs.getString("movies", "[]") ?: "[]"

        try {
            val moviesArray = JSONArray(moviesJson)

            // التحقق مما إذا كان الفيلم موجودًا بالفعل
            var alreadyExists = false
            for (i in 0 until moviesArray.length()) {
                val movie = moviesArray.getJSONObject(i)
                if (movie.optInt("id") == id) {
                    alreadyExists = true
                    break
                }
            }

            // إضافة الفيلم إذا لم يكن موجودًا
            if (!alreadyExists) {
                val movieObject = JSONObject()
                movieObject.put("id", id)
                movieObject.put("title", title)
                movieObject.put("poster", posterPath)
                movieObject.put("genre", genre)

                moviesArray.put(movieObject)

                // حفظ المصفوفة المحدثة
                prefs.edit().putString("movies", moviesArray.toString()).apply()
            }
        } catch (e: Exception) {
            Toast.makeText(this, "Error saving searched movie", Toast.LENGTH_SHORT).show()
        }
    }

    // عرض إشعار باقتراح فيلم عشوائي
    private fun showRandomMovieSuggestion() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
            != PackageManager.PERMISSION_GRANTED) {
            return
        }

        val randomMovie = getRandomSearchedMovie()
        val randomGenre = getRandomSearchedGenre()

        if (randomMovie != null) {
            // إنشاء intent لفتح تفاصيل الفيلم عند النقر على الإشعار
            val intent = Intent(this, MainActivity::class.java)
            intent.putExtra("movie_id", randomMovie.id)
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP

            val pendingIntent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
            } else {
                PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT)
            }

            // إنشاء الإشعار مع اسم الفيلم
            val builder = NotificationCompat.Builder(this, SUGGESTION_CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setContentTitle("🎬 اقتراح فيلم")
                .setContentText("هل تريد مشاهدة فيلم ${randomMovie.title}؟")
                .setContentIntent(pendingIntent)
                .setAutoCancel(true)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)

            with(NotificationManagerCompat.from(this)) {
                notify(1003, builder.build())
            }
        } else if (randomGenre != null) {
            // إشعار بنوع الفيلم إذا لم تكن هناك أفلام محددة
            val builder = NotificationCompat.Builder(this, SUGGESTION_CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setContentTitle("🎬 اقتراح فيلم")
                .setContentText("هل تحب مشاهدة فيلم $randomGenre؟")
                .setAutoCancel(true)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)

            with(NotificationManagerCompat.from(this)) {
                notify(1004, builder.build())
            }
        }
    }

    // الحصول على فيلم عشوائي من الأفلام التي تم البحث عنها
    private fun getRandomSearchedMovie(): MovieSuggestion? {
        val prefs = getSharedPreferences("searched_movies", MODE_PRIVATE)
        val moviesJson = prefs.getString("movies", "[]") ?: "[]"

        try {
            val moviesArray = JSONArray(moviesJson)
            if (moviesArray.length() > 0) {
                val randomIndex = Random.nextInt(moviesArray.length())
                val movie = moviesArray.getJSONObject(randomIndex)

                return MovieSuggestion(
                    id = movie.optInt("id"),
                    title = movie.optString("title"),
                    poster = movie.optString("poster", "")
                )
            }
        } catch (e: Exception) {
            // لا شيء للقيام به، سنعود إلى اقتراح النوع
        }

        return null
    }

    // كلاس لتخزين بيانات اقتراح الفيلم
    data class MovieSuggestion(val id: Int, val title: String, val poster: String)

    private fun resetMovieDetails() {
        currentMovieId = -1
        currentMovieTitle = ""
        currentMoviePoster = ""
        trailerVideoId = ""
        trailerButton.isEnabled = false
        favoriteButton.isEnabled = false
        ratingText.text = ""
        posterImage.setImageDrawable(null)
        reviewsText.text = ""
    }

    private fun fetchMovieDetails(movieId: Int) {
        val url = "https://api.themoviedb.org/3/movie/$movieId?api_key=2ed7b6b728c2ff496be456864d2896dd"
        val request = JsonObjectRequest(url, { movie ->
            try {
                val title = movie.optString("title", "No title")
                val overview = movie.optString("overview", "No overview")
                val rating = movie.optDouble("vote_average", 0.0)

                // Store current movie details
                currentMovieId = movieId
                currentMovieTitle = title

                // Clear previous content
                imagesContainer.removeAllViews()

                // عرض اسم الفيلم والتقييم والنبذة
                resultText.text = "🎬 $title\n\n📃 $overview"
                ratingText.text = "Rating: $rating ⭐"

                // عرض البوستر
                val posterPath = movie.optString("poster_path", "")
                if (posterPath.isNotEmpty()) {
                    currentMoviePoster = posterPath
                    val imageUrl = "https://image.tmdb.org/t/p/w500$posterPath"
                    Glide.with(this).load(imageUrl).into(posterImage)
                } else {
                    posterImage.setImageDrawable(null)
                    currentMoviePoster = ""
                }

                // Enable favorite button
                favoriteButton.isEnabled = true

                // Check if movie is already favorite
                updateFavoriteButtonText()

                // باقي التفاصيل
                fetchTrailer(movieId)
                fetchImages(movieId)
                fetchReviews(movieId)
                fetchSimilarMovies(movieId)

            } catch (e: Exception) {
                Toast.makeText(this, "Error in movie details", Toast.LENGTH_SHORT).show()
            }
        }, { onErrorResponse(it) })
        queue.add(request)
    }

    fun onResponse(response: JSONObject?) {
        try {
            if (response != null) {
                val results = response.getJSONArray("results")
                if (results.length() > 0) {
                    val movie = results.getJSONObject(0)
                    val movieId = movie.getInt("id")

                    // عرض نبذة الفيلم
                    resultText.text = movie.getString("overview")

                    // عرض التقييم
                    val rating = movie.getDouble("vote_average")
                    ratingText.text = "Rating: $rating ⭐"

                    // عرض بوستر الفيلم
                    val posterPath = movie.getString("poster_path")
                    val imageUrl = "https://image.tmdb.org/t/p/w500$posterPath"
                    Glide.with(this).load(imageUrl).into(posterImage)

                    // جلب الفيديوهات والصور والمراجعات والأفلام المشابهة
                    fetchTrailer(movieId)
                    fetchImages(movieId)
                    fetchReviews(movieId)
                    fetchSimilarMovies(movieId)

                } else {
                    resultText.text = "Movie not found"
                }
            }
        } catch (e: Exception) {
            Toast.makeText(this, "Error..try again", Toast.LENGTH_SHORT).show()
        }
    }

    fun onErrorResponse(error: com.android.volley.VolleyError?) {
        Toast.makeText(this, "Connection error", Toast.LENGTH_SHORT).show()
    }

    private fun fetchTrailer(movieId: Int) {
        val trailerUrl = "https://api.themoviedb.org/3/movie/$movieId/videos?api_key=2ed7b6b728c2ff496be456864d2896dd"
        val request = JsonObjectRequest(trailerUrl, { response ->
            val results = response.getJSONArray("results")
            var foundTrailer = false
            for (i in 0 until results.length()) {
                val video = results.getJSONObject(i)
                if (video.getString("site") == "YouTube" && video.getString("type") == "Trailer") {
                    trailerVideoId = video.getString("key")
                    foundTrailer = true
                    break
                }
            }
            trailerButton.isEnabled = foundTrailer
        }, { error -> Toast.makeText(this, "Error fetching trailer", Toast.LENGTH_SHORT).show() })
        queue.add(request)
    }

    private fun fetchSimilarMovies(movieId: Int) {
        val url = "https://api.themoviedb.org/3/movie/$movieId/similar?api_key=2ed7b6b728c2ff496be456864d2896dd"
        val request = JsonObjectRequest(url, { response ->
            val results = response.getJSONArray("results")
            val sb = StringBuilder()
            sb.append("\n\n🎬 Similar Movies:\n")
            for (i in 0 until minOf(5, results.length())) {
                val movie = results.getJSONObject(i)
                val similarMovieId = movie.getInt("id")
                val title = movie.getString("title")
                sb.append("• $title\n")
            }
            resultText.append(sb.toString())
        }, { error ->
            Toast.makeText(this, "Failed to fetch similar movies", Toast.LENGTH_SHORT).show()
        })
        queue.add(request)
    }

    private fun fetchReviews(movieId: Int) {
        val url = "https://api.themoviedb.org/3/movie/$movieId/reviews?api_key=2ed7b6b728c2ff496be456864d2896dd"
        val request = JsonObjectRequest(url, { response ->
            val reviews = response.getJSONArray("results")
            val sb = StringBuilder()
            for (i in 0 until minOf(3, reviews.length())) {
                val review = reviews.getJSONObject(i)
                val author = review.getString("author")
                val content = review.getString("content")
                sb.append("👤 $author:\n$content\n\n")
            }
            reviewsText.text = if (reviews.length() > 0) "Reviews:\n$sb" else "No reviews available"
        }, { error ->
            Toast.makeText(this, "Error loading reviews", Toast.LENGTH_SHORT).show()
        })
        queue.add(request)
    }

    private fun fetchImages(movieId: Int) {
        val url = "https://api.themoviedb.org/3/movie/$movieId/images?api_key=2ed7b6b728c2ff496be456864d2896dd"
        val request = JsonObjectRequest(url, { response ->
            val backdrops = response.getJSONArray("backdrops")
            imagesContainer.removeAllViews()

            val imagesTitle = TextView(this)
            imagesTitle.text = "Movie Images:"
            imagesTitle.textSize = 18f
            imagesTitle.setPadding(0, 16, 0, 8)
            imagesContainer.addView(imagesTitle)

            if (backdrops.length() > 0) {
                for (i in 0 until minOf(5, backdrops.length())) {
                    val imageObj = backdrops.getJSONObject(i)
                    val imagePath = imageObj.getString("file_path")
                    val fullImageUrl = "https://image.tmdb.org/t/p/w300$imagePath"
                    val imageView = ImageView(this)
                    imageView.layoutParams = LinearLayout.LayoutParams(400, 250)
                    imageView.setPadding(8, 8, 8, 8)
                    imageView.scaleType = ImageView.ScaleType.CENTER_CROP
                    Glide.with(this).load(fullImageUrl).into(imageView)
                    imagesContainer.addView(imageView)
                }
            } else {
                val noImagesText = TextView(this)
                noImagesText.text = "No images available"
                imagesContainer.addView(noImagesText)
            }
        }, { error ->
            Toast.makeText(this, "Error loading images", Toast.LENGTH_SHORT).show()
        })
        queue.add(request)
    }

    private fun playTrailerWithId(videoId: String) {
        val youtubeAppUri = Uri.parse("vnd.youtube:$videoId")
        val youtubeWebUri = Uri.parse("https://www.youtube.com/watch?v=$videoId")
        val appIntent = Intent(Intent.ACTION_VIEW, youtubeAppUri)
        val webIntent = Intent(Intent.ACTION_VIEW, youtubeWebUri)

        try {
            startActivity(appIntent)
        } catch (e: ActivityNotFoundException) {
            startActivity(webIntent)
        }
    }

    private fun incrementGenreCount(genre: String) {
        val prefs = getSharedPreferences("genre_prefs", MODE_PRIVATE)
        val count = prefs.getInt(genre, 0)
        prefs.edit().putInt(genre, count + 1).apply()
    }

    private fun getRandomSearchedGenre(): String? {
        val prefs = getSharedPreferences("genre_prefs", MODE_PRIVATE)
        val allGenres = prefs.all
            .filterValues { it is Int && it as Int > 0 }
            .map { it.key to it.value as Int }

        if (allGenres.isNotEmpty()) {
            return allGenres.random().first // genre name
        }
        return null
    }

    // Save movie to favorites using SharedPreferences
    private fun saveMovieAsFavorite() {
        if (currentMovieId == -1 || currentMovieTitle.isEmpty()) {
            Toast.makeText(this, "No movie selected", Toast.LENGTH_SHORT).show()
            return
        }

        val prefs = getSharedPreferences("favorite_movies", MODE_PRIVATE)
        val editor = prefs.edit()

        // Get existing favorites as JSON string
        val favoritesJson = prefs.getString("favorites", "[]") ?: "[]"

        try {
            // Parse existing favorites
            val favoritesArray = org.json.JSONArray(favoritesJson)

            // Check if movie is already in favorites
            var alreadyExists = false
            for (i in 0 until favoritesArray.length()) {
                val movie = favoritesArray.getJSONObject(i)
                if (movie.getInt("id") == currentMovieId) {
                    alreadyExists = true
                    break
                }
            }

            if (alreadyExists) {
                // Remove from favorites
                val updatedFavorites = org.json.JSONArray()
                for (i in 0 until favoritesArray.length()) {
                    val movie = favoritesArray.getJSONObject(i)
                    if (movie.getInt("id") != currentMovieId) {
                        updatedFavorites.put(movie)
                    }
                }
                editor.putString("favorites", updatedFavorites.toString())
                Toast.makeText(this, "Removed from favorites", Toast.LENGTH_SHORT).show()
            } else {
                // Add to favorites
                val movieObject = JSONObject()
                movieObject.put("id", currentMovieId)
                movieObject.put("title", currentMovieTitle)
                movieObject.put("poster", currentMoviePoster)

                favoritesArray.put(movieObject)
                editor.putString("favorites", favoritesArray.toString())
                Toast.makeText(this, "Added to favorites", Toast.LENGTH_SHORT).show()
            }

            editor.apply()
            updateFavoriteButtonText()

        } catch (e: Exception) {
            Toast.makeText(this, "Error saving favorite", Toast.LENGTH_SHORT).show()
        }
    }

    private fun updateFavoriteButtonText() {
        if (currentMovieId == -1) {
            favoriteButton.text = "Add to Favorites"
            return
        }

        val prefs = getSharedPreferences("favorite_movies", MODE_PRIVATE)
        val favoritesJson = prefs.getString("favorites", "[]") ?: "[]"

        try {
            val favoritesArray = org.json.JSONArray(favoritesJson)
            var isFavorite = false

            for (i in 0 until favoritesArray.length()) {
                val movie = favoritesArray.getJSONObject(i)
                if (movie.getInt("id") == currentMovieId) {
                    isFavorite = true
                    break
                }
            }

            favoriteButton.text = if (isFavorite) "Remove from Favorites" else "Add to Favorites"

        } catch (e: Exception) {
            favoriteButton.text = "Add to Favorites"
        }
    }

    private fun showFavoriteMovies() {
        val prefs = getSharedPreferences("favorite_movies", MODE_PRIVATE)
        val favoritesJson = prefs.getString("favorites", "[]") ?: "[]"

        try {
            val favoritesArray = org.json.JSONArray(favoritesJson)

            // Clear current display
            imagesContainer.removeAllViews()
            resultText.text = "Your Favorite Movies"
            reviewsText.text = ""
            posterImage.setImageDrawable(null)
            ratingText.text = ""
            trailerButton.isEnabled = false
            favoriteButton.isEnabled = false

            if (favoritesArray.length() == 0) {
                val noFavoritesText = TextView(this)
                noFavoritesText.text = "You don't have any favorite movies yet"
                noFavoritesText.textSize = 16f
                imagesContainer.addView(noFavoritesText)
                return
            }

            for (i in 0 until favoritesArray.length()) {
                val movie = favoritesArray.getJSONObject(i)
                val movieId = movie.getInt("id")
                val title = movie.getString("title")
                val posterPath = movie.optString("poster", "")

                val movieContainer = LinearLayout(this)
                movieContainer.orientation = LinearLayout.VERTICAL
                movieContainer.setPadding(0, 16, 0, 16)

                val titleText = TextView(this)
                titleText.text = "🎬 $title"
                titleText.textSize = 18f
                titleText.setPadding(8, 8, 8, 8)
                movieContainer.addView(titleText)

                if (posterPath.isNotEmpty()) {
                    val imageUrl = "https://image.tmdb.org/t/p/w200$posterPath"
                    val imageView = ImageView(this)
                    imageView.layoutParams = LinearLayout.LayoutParams(300, 450)
                    imageView.scaleType = ImageView.ScaleType.CENTER_CROP
                    Glide.with(this).load(imageUrl).into(imageView)
                    movieContainer.addView(imageView)
                }

                val viewButton = Button(this)
                viewButton.text = "View Details"
                viewButton.setOnClickListener {
                    fetchMovieDetails(movieId)
                }
                movieContainer.addView(viewButton)

                val divider = View(this)
                divider.layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, 2
                )
                divider.setBackgroundColor(Color.LTGRAY)

                imagesContainer.addView(movieContainer)
                imagesContainer.addView(divider)
            }

        } catch (e: Exception) {
            Toast.makeText(this, "Error loading favorites", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onResume() {
        super.onResume()

        val suggestedGenre = getRandomSearchedGenre()
        suggestedGenre?.let {
            showGenreNotification(it)
        }
    }

    // إضافة دالة عرض إشعار باقتراح أفلام من نوع معين
    private fun showGenreNotification(genre: String) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
            != PackageManager.PERMISSION_GRANTED) {
            return
        }

        // إنشاء intent لفتح التطبيق مع اختيار هذا النوع من الأفلام
        val intent = Intent(this, MainActivity::class.java)
        // نضيف النوع كمعلمة إضافية للقصد لنتمكن من استخدامه لاحقًا
        intent.putExtra("selected_genre", genre)
        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP

        val pendingIntent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        } else {
            PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT)
        }

        // إنشاء الإشعار باقتراح فيلم من هذا النوع
        val builder = NotificationCompat.Builder(this, SUGGESTION_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("🎬 اقتراح أفلام")
            .setContentText("هل تحب مشاهدة أفلام $genre اليوم؟")
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)

        with(NotificationManagerCompat.from(this)) {
            notify(1005, builder.build())
        }
    }

}