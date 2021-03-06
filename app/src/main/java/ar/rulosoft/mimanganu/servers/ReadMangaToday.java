package ar.rulosoft.mimanganu.servers;

import android.content.Context;

import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import ar.rulosoft.mimanganu.R;
import ar.rulosoft.mimanganu.componentes.Chapter;
import ar.rulosoft.mimanganu.componentes.Manga;
import ar.rulosoft.mimanganu.componentes.ServerFilter;
import ar.rulosoft.mimanganu.utils.Util;

/**
 * Created by xtj-9182 on 01.12.2016.
 */
class ReadMangaToday extends ServerBase {
    private static String HOST = "http://www.readmanga.today";
    private static String[] genre = new String[]{
            "All",
            "Action",
            "Adventure",
            "Comedy",
            "Doujinshi",
            "Drama",
            "Ecchi",
            "Fantasy",
            "Gender Bender",
            "Harem",
            "Historical",
            "Horror",
            "Josei",
            "Lolicon",
            "Martial Arts",
            "Mature",
            "Mecha",
            "Mystery",
            "One shot",
            "Psychological",
            "Romance",
            "School Life",
            "Sci-fi",
            "Seinen",
            "Shotacon",
            "Shoujo",
            "Shoujo Ai",
            "Shounen",
            "Shounen Ai",
            "Slice of Life",
            "Smut",
            "Sports",
            "Supernatural",
            "Tragedy",
            "Yaoi",
            "Yuri"
    };
    private static String genreVV = "/category/";

    ReadMangaToday(Context context) {
        super(context);
        this.setFlag(R.drawable.flag_en);
        this.setIcon(R.drawable.readmangatoday);
        this.setServerName("ReadMangaToday");
        setServerID(ServerBase.READMANGATODAY);
    }

    @Override
    public ArrayList<Manga> getMangas() throws Exception {
        return null;
    }

    @Override
    public ArrayList<Manga> search(String search) throws Exception {
        String web = "http://www.readmanga.today/manga-list/";
        if (Character.isLetter(search.charAt(0)))
            web = web + search.toLowerCase().charAt(0);
        //Log.d("RMT", "web: " + web);
        String source = getNavigatorAndFlushParameters().get(web);
        Pattern pattern = Pattern.compile("<a href=\"(http://www.readmanga.today/[^\"]+?)\">(.+?)</a>");
        Matcher matcher = pattern.matcher(source);
        ArrayList<Manga> mangas = new ArrayList<>();
        while (matcher.find()) {
            if (matcher.group(2).toLowerCase().contains(search.toLowerCase())) {
                /*Log.d("RMT", "1: " + matcher.group(1));
                Log.d("RMT", "2: " + matcher.group(2));*/
                Manga manga = new Manga(getServerID(), matcher.group(2), matcher.group(1), false);
                mangas.add(manga);
            }
        }
        return mangas;
    }

    @Override
    public void loadChapters(Manga manga, boolean forceReload) throws Exception {
        if (manga.getChapters() == null || manga.getChapters().size() == 0 || forceReload)
            loadMangaInformation(manga, forceReload);
    }

    @Override
    public void loadMangaInformation(Manga manga, boolean forceReload) throws Exception {
        String source = getNavigatorAndFlushParameters().get(manga.getPath());

        // Front
        String img = getFirstMatchDefault("<div class=\"col-md-3\">.+?<img src=\"(.+?)\" alt=", source, "");
        manga.setImages(img);

        // Summary
        String summary = getFirstMatchDefault("<li class=\"list-group-item movie-detail\">(.+?)</li>", source, "");
        manga.setSynopsis(Util.getInstance().fromHtml(summary.trim()).toString());

        // Status
        boolean status = !getFirstMatchDefault("<dt>Status:</dt>.+?<dd>(.+?)</dd>", source, "").contains("Ongoing");
        manga.setFinished(status);

        // Author
        String author = getFirstMatchDefault("<li class=\"director\">.+?<li><a href=\".+?\">(.+?)</a>", source, "");
        manga.setAuthor(author);

        // Genre
        String genre = Util.getInstance().fromHtml(getFirstMatchDefault("<dt>Categories:</dt>.+?<dd>(.+?)</dd>", source, "")).toString().trim().replaceAll(" ", ", ");
        manga.setGenre(genre);

        // Chapters
        //<li>.+?<a href="(.+?)">.+?<span class="val"><span class="icon-arrow-2"></span>(.+?)</span>
        Pattern p = Pattern.compile("<li>[\\s]*<a href=\"([^\"]+?)\">[\\s]*<span class=\"val\"><span class=\"icon-arrow-.\"></span>(.+?)</span>");
        Matcher matcher = p.matcher(source);
        ArrayList<Chapter> chapters = new ArrayList<>();
        while (matcher.find()) {
            /*Log.d("RMT", "(2): " + matcher.group(2).trim());
            Log.d("RMT", "(1): " + matcher.group(1));*/
            chapters.add(0, new Chapter(matcher.group(2).trim(), matcher.group(1)));
        }
        manga.setChapters(chapters);
    }

    @Override
    public String getPagesNumber(Chapter chapter, int page) {
        return chapter.getPath() + "/" + page;
    }

    @Override
    public String getImageFrom(Chapter chapter, int page) throws Exception {
        if (chapter.getExtra() == null)
            setExtra(chapter);
        String[] images = chapter.getExtra().split("\\|");
        return images[page];
    }

    private void setExtra(Chapter chapter) throws Exception {
        String source = getNavigatorAndFlushParameters().get(chapter.getPath() + "/all-pages");
        Pattern p = Pattern.compile("<img src=\"([^\"]+)\" class=\"img-responsive-2\">");
        Matcher matcher = p.matcher(source);
        String images = "";
        while (matcher.find()) {
            //Log.d("RMT","(1): "+matcher.group(1));
            images = images + "|" + matcher.group(1);
        }
        chapter.setExtra(images);
    }

    @Override
    public void chapterInit(Chapter chapter) throws Exception {
        String source = getNavigatorAndFlushParameters().get(chapter.getPath());
        //Log.d("RMT","p: "+chapter.getPath());
        String pagenumber = getFirstMatch("\">(\\d+)</option>[\\s]*</select>", source,
                "failed to get the number of pages");
        //Log.d("RMT","pa: "+pagenumber);
        chapter.setPages(Integer.parseInt(pagenumber));
    }

    @Override
    public ServerFilter[] getServerFilters() {
        return new ServerFilter[]{
                new ServerFilter("Genre(s)", genre, ServerFilter.FilterType.SINGLE),
        };
    }

    @Override
    public ArrayList<Manga> getMangasFiltered(int[][] filters, int pageNumber) throws Exception {
        String web;
        if (genre[filters[0][0]].equals("All")) {
            if(pageNumber == 1)
                web = HOST + "/hot-manga/";
            else
                web = HOST + "/hot-manga/" + pageNumber;
        }
        else
            web = HOST + genreVV + genre[filters[0][0]].toLowerCase().replaceAll(" ","-") +"/"+ pageNumber;
        //Log.d("RMT", "web: " + web);
        String source = getNavigatorAndFlushParameters().get(web);
        // regex to generate genre ids: <li>.+?title="All Categories - (.+?)">
        Pattern pattern = Pattern.compile("<div class=\"left\">.+?<a href=\"(.+?)\" title=\"(.+?)\"><img src=\"(.+?)\" alt=\"");
        Matcher matcher = pattern.matcher(source);
        ArrayList<Manga> mangas = new ArrayList<>();
        while (matcher.find()) {
            /*Log.d("RMT","(2): "+matcher.group(2));
            Log.d("RMT","(1): "+matcher.group(1));
            Log.d("RMT","(3): "+matcher.group(3));*/
            Manga m = new Manga(getServerID(), matcher.group(2), matcher.group(1), false);
            m.setImages(matcher.group(3));
            mangas.add(m);
        }
        return mangas;
    }

    @Override
    public boolean hasList() {
        return false;
    }
}

