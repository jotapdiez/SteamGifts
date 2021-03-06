package net.mabako.steam.store;

import android.os.AsyncTask;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import net.mabako.Constants;
import net.mabako.steam.store.data.Picture;
import net.mabako.steam.store.data.Space;
import net.mabako.steam.store.data.Text;
import net.mabako.steamgifts.adapters.IEndlessAdaptable;
import net.mabako.steamgifts.core.R;

import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.Connection;
import org.jsoup.Jsoup;

import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class StoreAppFragment extends StoreFragment {
    private static final String TAG = StoreAppFragment.class.getSimpleName();

    public static StoreAppFragment newInstance(int appId, boolean refreshOnCreate) {
        StoreAppFragment fragment = new StoreAppFragment();

        Bundle args = new Bundle();
        args.putString("app", String.valueOf(appId));
        args.putBoolean("refresh", refreshOnCreate);
        fragment.setArguments(args);

        return fragment;
    }

    @Override
    public void onResume() {
        super.onResume();
        if (getArguments().getBoolean("refresh", false))
            refresh();
    }

    @Override
    protected AsyncTask<Void, Void, ?> getFetchItemsTask(int page) {
        return new LoadAppTask();
    }

    private class LoadAppTask extends LoadStoreTask {
        @Override
        protected Connection getConnection() {
            return Jsoup
                    .connect("http://store.steampowered.com/api/appdetails/")
                    .userAgent(Constants.JSOUP_USER_AGENT)
                    .timeout(Constants.JSOUP_TIMEOUT)
                    .data("appids", getArguments().getString("app"))
                    .data("l", "en");
        }

        @Override
        protected void onPostExecute(JSONObject jsonObject) {
            if (jsonObject != null) {
                try {
                    JSONObject sub = jsonObject.getJSONObject(getArguments().getString("app"));

                    // Were we successful in fetching the details?
                    if (sub.getBoolean("success")) {
                        JSONObject data = sub.getJSONObject("data");

                        List<IEndlessAdaptable> items = new ArrayList<IEndlessAdaptable>();

                        // Game name
                        items.add(new Text("<h1>" + TextUtils.htmlEncode(data.getString("name")) + "</h1>", true));

                        // Game description.
                        if (data.has("about_the_game")) {
                            String txt = data.getString("about_the_game");
                            items.addAll(processText(txt));
                        }

                        // Release?
                        if (data.has("release_date"))
                            items.add(new Text("<strong>Release:</strong> " + data.getJSONObject("release_date").getString("date"), true, true));

                        // Genres
                        if (data.has("genres")) {
                            JSONArray genres = data.getJSONArray("genres");
                            if (genres.length() > 0) {
                                StringBuilder sb = new StringBuilder("<strong>Genre:</strong> ");
                                for (int i = 0; i < genres.length(); ++i) {
                                    if (i > 0)
                                        sb.append(", ");

                                    sb.append(genres.getJSONObject(i).getString("description"));
                                }
                                items.add(new Text(sb.toString(), true));
                            }
                        }

                        // Categories (icons)
                        if (data.has("categories")){
                            JSONArray categories = data.getJSONArray("categories");
                            StringBuilder sb = new StringBuilder();
                            for (int i = 0; i < categories.length(); ++i) {
                                JSONObject cat = categories.getJSONObject(i);
                                String icoName = "";
                                switch (cat.getInt("id")){
                                    case 1: //Multijugador
                                    case 27: //Multijugador multiplataforma
                                    case 36: //Multijugador en línea
                                    case 37: //Multijugador local
                                        icoName = "ico_multiPlayer";
                                        break;
                                    case 9: //Cooperativo
                                    case 24: //Shared/Split Screen
                                    case 38: //Online Co-op
                                    case 39: //Local Co-op
                                        icoName = "ico_coop";
                                        break;
                                    case 2: //Un jugador
                                        icoName = "ico_singlePlayer";
                                        break;
                                    case 8: //Con sist. antitrampas de Valve
                                        icoName = "ico_vac";
                                        break;
                                    case 13: //Subtítulos disponibles
                                        icoName = "ico_cc";
                                        break;
                                    case 14: //Comentario disponible
                                        icoName = "ico_commentary";
                                        break;
                                    case 15: //Estadísticas
                                        icoName = "ico_stats";
                                        break;
                                    case 17: //Incluye editor de niveles
                                        icoName = "ico_editor";
                                        break;
                                    case 18: //Compat. parcial con mando
                                        icoName = "ico_partial_controller";
                                        break;
                                    case 21: //Downloadable Content
                                        icoName = "ico_dlc";
                                        break;
                                    case 22: //Logros
                                        icoName = "ico_achievements";
                                        break;
                                    case 23: //Steam Cloud
                                        icoName = "ico_cloud";
                                        break;
                                    case 25: //Steam Leaderboards
                                        icoName = "ico_leaderboards";
                                        break;
                                    case 28: //Full controller support
                                        icoName = "ico_controller";
                                        break;
                                    case 29: //Cromos
                                        icoName = "ico_cards";
                                        break;
                                    case 30: //Steam Workshop
                                        icoName = "ico_workshop";
                                        break;
                                    case 35: //Compras dentro de la aplicación
                                        icoName = "ico_cart";
                                        break;
                                }

                                if (icoName.equals("")){
                                    Log.d(TAG, "icono de Categories no contemplado. ID:"+cat.getInt("id"));
                                }else {
                                    sb.append("<img src=\"https://steamstore-a.akamaihd.net/public/images/v6/ico/"+icoName+".png\" style=\"width: 26px; height: 16px;\"/>");
                                }
                            }
                            items.add(new Text(sb.toString(), true));
                        }

                        // Space!
                        items.add(new Space());

                        // Some screenshots
                        if (data.has("screenshots")) {
                            JSONArray screenshots = data.getJSONArray("screenshots");
                            for (int i = 0; i < screenshots.length(); ++i) {
                                items.add(new Picture(screenshots.getJSONObject(i).getString("path_thumbnail")));
                            }
                        }

                        if (data.has("legal_notice"))
                            items.add(new Text(data.getString("legal_notice"), true, R.layout.endless_scroll_end, false));

                        addItems(items, true);
                    } else throw new Exception("not successful");
                } catch (Exception e) {
                    Log.e(TAG, "Exception during loading store app", e);
                    Toast.makeText(getContext(), "Unable to load Store App", Toast.LENGTH_LONG).show();
                }
            } else {
                Log.e(TAG, "no JSON object");
                Toast.makeText(getContext(), "Unable to load Store App", Toast.LENGTH_LONG).show();
            }

            getView().findViewById(R.id.progressBar).setVisibility(View.GONE);
        }
    }

    private List<IEndlessAdaptable> processText(String the_game) {
        List<IEndlessAdaptable> items = new ArrayList<IEndlessAdaptable>();

        String result = the_game.replaceAll("<br>", "<br/>\n");

        Pattern p = Pattern.compile("(<img.*src=\"([^\"]*)\"[\\s]*>)");
        Matcher m = p.matcher(result);

        int index = 0;
        int imgIndex = 0;
        while (m.find()) {
            String codeGroup = m.group(1);
            imgIndex = result.indexOf(codeGroup);

            String text = result.substring(index, imgIndex);
            //Log.d(TAG, "** about_the_game::text: " + text);
            items.add(new Text(text, true));

            String img = m.group(2);
            //Log.d(TAG, "** about_the_game::Img: " + img);
            items.add(new Picture(img));

            index = imgIndex+ codeGroup.length();
        }

        if (result.length() > index){
            String text = result.substring(index);
            //Log.d(TAG, "** about_the_game::Ultimo text: " + text);
            items.add(new Text(text, true));
        }

        return items;
    }
}
