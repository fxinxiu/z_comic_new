package com.android.zhhr.module;

import android.app.Activity;
import android.content.Context;
import android.util.Log;

import com.android.zhhr.data.commons.Constants;
import com.android.zhhr.data.commons.Url;
import com.android.zhhr.data.entity.Comic;
import com.android.zhhr.data.entity.HomeTitle;
import com.android.zhhr.data.entity.HttpResult;
import com.android.zhhr.data.entity.SearchBean;
import com.android.zhhr.data.entity.db.DBSearchResult;
import com.android.zhhr.db.helper.DaoHelper;
import com.android.zhhr.net.ComicService;
import com.android.zhhr.net.HttpResultFunc;
import com.android.zhhr.net.MainFactory;
import com.android.zhhr.utils.ApiException;
import com.android.zhhr.utils.DBEntityUtils;
import com.android.zhhr.utils.TencentComicAnalysis;
import com.trello.rxlifecycle.ActivityEvent;
import com.trello.rxlifecycle.components.support.RxAppCompatActivity;
import com.trello.rxlifecycle.components.support.RxFragment;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import rx.Observable;
import rx.Subscriber;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Func1;
import rx.schedulers.Schedulers;

/**
 * Created by 皓然 on 2017/7/31.
 */

public class ComicModule {
    public static final ComicService comicService = MainFactory.getComicServiceInstance();
    public Context context;
    private DaoHelper mHelper;
    private RxAppCompatActivity rxAppCompatActivity;
    public ComicModule(Activity context){
        rxAppCompatActivity = (RxAppCompatActivity) context;
        mHelper = new DaoHelper(context);
    }
    //首页相关
    public void getData(Subscriber subscriber){
        Observable ComicListObservable = Observable.create(new Observable.OnSubscribe<List<Comic>>() {
            @Override
            public void call(Subscriber<? super List<Comic>> subscriber) {
                try {
                    List<Comic>  mdats = new ArrayList<>();
                    Document recommend = Jsoup.connect(Url.TencentHomePage).get();
                    Document japan = Jsoup.connect(Url.TencentJapanHot).get();
                    Document doc = Jsoup.connect(Url.TencentTopUrl+"1").get();
                    addComic(recommend,mdats,Constants.TYPE_RECOMMEND);
                    addComic(recommend,mdats,Constants.TYPE_BOY_RANK);
                    addComic(recommend,mdats,Constants.TYPE_GIRL_RANK);
                    addComic(recommend,mdats,Constants.TYPE_HOT_SERIAL);
                    addComic(japan,mdats,Constants.TYPE_HOT_JAPAN);
                    addComic(doc,mdats,Constants.TYPE_RANK_LIST);

                    HomeTitle homeTitle = new HomeTitle();
                    homeTitle.setItemTitle("");
                    mdats.add(homeTitle);
                    subscriber.onNext(mdats);
                } catch (IOException e) {
                    subscriber.onError(e);
                    e.printStackTrace();
                }finally {
                    subscriber.onCompleted();
                }
            }
        });
        Observable ComicBannerObservable = Observable.create(new Observable.OnSubscribe<List<Comic>>() {
            @Override
            public void call(Subscriber<? super List<Comic>> subscriber) {
                try {
                    Document doc = Jsoup.connect(Url.TencentBanner).get();
                    List<Comic>  mdats = TencentComicAnalysis.TransToBanner(doc);
                    subscriber.onNext(mdats);
                } catch (IOException e) {
                    subscriber.onError(e);
                    e.printStackTrace();
                }finally {
                    subscriber.onCompleted();
                }
            }
        });
        Observable.merge(ComicListObservable, ComicBannerObservable).subscribeOn(Schedulers.io())
                .unsubscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .compose(rxAppCompatActivity.bindUntilEvent(ActivityEvent.DESTROY))
                .subscribe(subscriber);
    }

    /**
     * 添加漫画到List里
     * @param doc
     * @param mdats
     * @param type
     */
    private void addComic(Document doc, List<Comic> mdats, int type) {
        HomeTitle homeTitle;
        try {
            switch (type){
                case Constants.TYPE_RECOMMEND:
                    homeTitle = new HomeTitle();
                    homeTitle.setItemTitle("强推作品");
                    homeTitle.setTitleType(Constants.TYPE_RECOMMEND);
                    mdats.add(homeTitle);
                    mdats.addAll(TencentComicAnalysis.TransToRecommendComic(doc));
                    break;
                case Constants.TYPE_BOY_RANK:
                    homeTitle = new HomeTitle();
                    homeTitle.setItemTitle("少年漫画");
                    homeTitle.setTitleType(Constants.TYPE_HOT_SERIAL);
                    mdats.add(homeTitle);
                    mdats.addAll(TencentComicAnalysis.TransToBoysComic(doc));
                    break;
                case Constants.TYPE_GIRL_RANK:
                    homeTitle = new HomeTitle();
                    homeTitle.setItemTitle("少女漫画");
                    homeTitle.setTitleType(Constants.TYPE_HOT_SERIAL);
                    mdats.add(homeTitle);
                    mdats.addAll(TencentComicAnalysis.TransToGirlsComic(doc));
                    break;
                case Constants.TYPE_HOT_SERIAL:
                    homeTitle = new HomeTitle();
                    homeTitle.setItemTitle("热门连载");
                    homeTitle.setTitleType(Constants.TYPE_HOT_SERIAL);
                    mdats.add(homeTitle);
                    mdats.addAll(TencentComicAnalysis.TransToNewComic(doc));
                    break;
                case Constants.TYPE_HOT_JAPAN:
                    homeTitle = new HomeTitle();
                    homeTitle.setItemTitle("日漫馆");
                    homeTitle.setTitleType(Constants.TYPE_HOT_JAPAN);
                    mdats.add(homeTitle);
                    mdats.addAll(TencentComicAnalysis.TransToJapanComic(doc));
                    break;
                case Constants.TYPE_RANK_LIST:
                    homeTitle = new HomeTitle();
                    homeTitle.setItemTitle("排行榜");
                    homeTitle.setTitleType(Constants.TYPE_RANK_LIST);
                    mdats.add(homeTitle);
                    mdats.addAll(TencentComicAnalysis.TransToComic(doc));
                    break;
            }
        }catch (Exception e){
            Log.d("zhhr","type = "+type+" is Error");
        }

    }

    public void refreshData(Subscriber subscriber){
        Observable ComicListObservable = Observable.create(new Observable.OnSubscribe<List<Comic>>() {
            @Override
            public void call(Subscriber<? super List<Comic>> subscriber) {
                try {
                    List<Comic>  mdats = new ArrayList<>();
                    HomeTitle homeTitle = new HomeTitle();
                    homeTitle.setItemTitle("排行榜");
                    mdats.add(homeTitle);

                    Document doc = Jsoup.connect(Url.TencentTopUrl+"1").get();
                    Document doc2 = Jsoup.connect(Url.TencentTopUrl+"2").get();

                    mdats.addAll(TencentComicAnalysis.TransToComic(doc));
                    mdats.addAll(TencentComicAnalysis.TransToComic(doc2));

                    homeTitle = new HomeTitle();
                    homeTitle.setItemTitle("");
                    mdats.add(homeTitle);
                    subscriber.onNext(mdats);
                } catch (IOException e) {
                    subscriber.onError(e);
                    e.printStackTrace();
                }finally {
                    subscriber.onCompleted();
                }
            }
        });
        ComicListObservable.subscribeOn(Schedulers.io())
                .unsubscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .compose(rxAppCompatActivity.bindUntilEvent(ActivityEvent.DESTROY))
                .subscribe(subscriber);
    }

    public void getMoreComicList(final int page, Subscriber subscriber){
        Observable.create(new Observable.OnSubscribe<List<Comic>>() {
            @Override
            public void call(Subscriber<? super List<Comic>> subscriber) {
                try {
                    Document doc = Jsoup.connect(Url.TencentTopUrl+page).get();
                    List<Comic> mdats = TencentComicAnalysis.TransToComic(doc);
                    subscriber.onNext(mdats);
                } catch (IOException e) {
                    subscriber.onError(e);
                    e.printStackTrace();
                }finally {
                    subscriber.onCompleted();
                }
            }
        }) .subscribeOn(Schedulers.io())
                .unsubscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .compose(rxAppCompatActivity.bindUntilEvent(ActivityEvent.DESTROY))
                .subscribe(subscriber);

    }

    //详情页相关
    public void getComicDetail(final String mComicId,Subscriber subscriber){
       Observable.create(new Observable.OnSubscribe<Comic>() {
            @Override
            public void call(Subscriber<? super Comic> subscriber) {
                try {
                    Document doc = Jsoup.connect(Url.TencentDetail+mComicId).get();
                    Comic mComic = TencentComicAnalysis.TransToComicDetail(doc,context);
                    Comic comicFromDB = (Comic) mHelper.findComic(Long.parseLong(mComicId));
                    if(comicFromDB!=null) {
                        mComic.setCurrentChapter(comicFromDB.getCurrentChapter());
                    }else{
                        mComic.setCurrentChapter(0);
                    }
                    subscriber.onNext(mComic);
                } catch (Exception e) {
                    subscriber.onError(e);
                    e.printStackTrace();
                }finally {
                    subscriber.onCompleted();
                }
            }
        }).subscribeOn(Schedulers.io())
               .unsubscribeOn(Schedulers.io())
               .observeOn(AndroidSchedulers.mainThread())
               .compose(rxAppCompatActivity.bindUntilEvent(ActivityEvent.DESTROY))
               .subscribe(subscriber);
    }

    //阅读相关
    public void getPreNowChapterList(String comic_id,int comic_chapters,Subscriber subscriber){
        comicService.getPreNowChapterList(comic_id,comic_chapters+"")
                .subscribeOn(Schedulers.io())
                .unsubscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .compose(rxAppCompatActivity.bindUntilEvent(ActivityEvent.DESTROY))
                .subscribe(subscriber);
    }

    public void getChaptersList(String comic_id,int comic_chapters,Subscriber subscriber){
        comicService.getChapters(comic_id,comic_chapters+"")
                .subscribeOn(Schedulers.io())
                .unsubscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .compose(rxAppCompatActivity.bindUntilEvent(ActivityEvent.DESTROY))
                .subscribe(subscriber);

    }
    //搜索相关

    //搜索相关
    public void getDynamicResult(String title,Subscriber subscriber){
        comicService.getDynamicSearchResult(Url.TencentSearchUrl+title)
                .map(new HttpResultFunc<List<SearchBean>>())
                .subscribeOn(Schedulers.io())
                .unsubscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .compose(rxAppCompatActivity.bindUntilEvent(ActivityEvent.DESTROY))
                .subscribe(subscriber);
    }



    public void getSearchResult(final String title, Subscriber subscriber) {
        Observable.create(new Observable.OnSubscribe<List<Comic>>() {
            @Override
            public void call(Subscriber<? super List<Comic>> subscriber) {
                try {
                    Document doc = Jsoup.connect(Url.TencentSearchResultUrl+title).get();
                    List<Comic> mdats = TencentComicAnalysis.TransToSearchResultComic(doc);
                    subscriber.onNext(mdats);
                } catch (Exception e) {
                    subscriber.onError(e);
                    e.printStackTrace();
                }finally {
                    subscriber.onCompleted();
                }
            }
        }) .subscribeOn(Schedulers.io())
                .unsubscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .compose(rxAppCompatActivity.bindUntilEvent(ActivityEvent.DESTROY))
                .subscribe(subscriber);
    }

    public void getTopResult(Subscriber subscriber) {
        Observable.create(new Observable.OnSubscribe<List<Comic>>() {
            @Override
            public void call(Subscriber<? super List<Comic>> subscriber) {
                try {
                    Document doc = Jsoup.connect(Url.TencentSearchRecommend).get();
                    List<Comic> mdats = TencentComicAnalysis.TransToSearchTopComic(doc);
                    subscriber.onNext(mdats);
                } catch (Exception e) {
                    subscriber.onError(e);
                    e.printStackTrace();
                } finally {
                    subscriber.onCompleted();
                }
            }
        }).subscribeOn(Schedulers.io())
                .unsubscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .compose(rxAppCompatActivity.bindUntilEvent(ActivityEvent.DESTROY))
                .subscribe(subscriber);
    }

    //操作数据库相关方法

    public void saveComicToDB(final Comic comic,Subscriber subscriber){
        Observable.create(new Observable.OnSubscribe<Boolean>() {
            @Override
            public void call(Subscriber<? super Boolean> subscriber) {
                try{
                    if(mHelper.findComic(comic.getId())==null){
                        if(mHelper.insert(comic)){
                            subscriber.onNext(true);
                        }else{
                            subscriber.onNext(false);
                        }
                    }else{
                        subscriber.onNext(false);
                    }
                }catch (Exception e){
                    subscriber.onError(e);
                }finally {
                    subscriber.onCompleted();
                }
            }
        }) .subscribeOn(Schedulers.io())
                .unsubscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .compose(rxAppCompatActivity.bindUntilEvent(ActivityEvent.DESTROY))
                .subscribe(subscriber);
    }

    public void updateComicCurrentChapter(final String comic_id, final int current_chapters, Subscriber subscriber){
        Observable.create(new Observable.OnSubscribe<Boolean>() {
            @Override
            public void call(Subscriber<? super Boolean> subscriber) {
                try{
                    Comic mComic = (Comic) mHelper.findComic(Long.parseLong(comic_id));
                    final java.util.Date date = new java.util.Date();
                    long datetime = date.getTime();
                    if(mComic!=null){
                        mComic.setCurrentChapter(current_chapters+1);
                        mComic.setUpdateTime(datetime);
                        if(mHelper.update(mComic)){
                            subscriber.onNext(true);
                        }else{
                            subscriber.onNext(false);
                        }
                    }else{
                        subscriber.onNext(false);
                    }
                }catch (Exception e){
                    subscriber.onError(e);
                }finally {
                    subscriber.onCompleted();
                }
            }
        }) .subscribeOn(Schedulers.io())
                .unsubscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .compose(rxAppCompatActivity.bindUntilEvent(ActivityEvent.DESTROY))
                .subscribe(subscriber);
    }

    public void updateComicToDB(final Comic mComic, Subscriber subscriber){
        Observable.create(new Observable.OnSubscribe<Boolean>() {
            @Override
            public void call(Subscriber<? super Boolean> subscriber) {
                try{
                    Comic comic = (Comic) mHelper.findComic(mComic.getId());
                    if(comic!=null){
                        if(mHelper.update(mComic)){
                            subscriber.onNext(true);
                        }else{
                            subscriber.onNext(false);
                        }
                    }else{
                        subscriber.onNext(false);
                    }
                }catch (Exception e){
                    subscriber.onError(e);
                }finally {
                    subscriber.onCompleted();
                }
            }
        }) .subscribeOn(Schedulers.io())
                .unsubscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .compose(rxAppCompatActivity.bindUntilEvent(ActivityEvent.DESTROY))
                .subscribe(subscriber);
    }

    public void isCollected(final long id,Subscriber subscriber) {
        Observable.create(new Observable.OnSubscribe<Boolean>() {
            @Override
            public void call(Subscriber<? super Boolean> subscriber) {
                try{
                    Comic mComic = (Comic) mHelper.findComic(id);
                    if(mComic!=null) {
                        if(mComic.getIsCollected()){
                            subscriber.onNext(false);
                        }else{
                            subscriber.onNext(true);
                        }
                    }else{
                        subscriber.onNext(true);
                    }
                }catch (Exception e){
                    subscriber.onError(e);
                }finally {
                    subscriber.onCompleted();
                }
            }
        }) .subscribeOn(Schedulers.io())
                .unsubscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .compose(rxAppCompatActivity.bindUntilEvent(ActivityEvent.DESTROY))
                .subscribe(subscriber);
    }

    public void getComicFromDB(final long id,Subscriber subscriber) {
        Observable.create(new Observable.OnSubscribe<Comic>() {
            @Override
            public void call(Subscriber<? super Comic> subscriber) {
                try{
                    Comic mComic = (Comic) mHelper.findComic(id);
                    if(mComic!=null) {
                        subscriber.onNext(mComic);
                    }else{
                        subscriber.onNext(null);
                    }
                }catch (Exception e){
                    subscriber.onError(e);
                }finally {
                    subscriber.onCompleted();
                }
            }
        }) .subscribeOn(Schedulers.io())
                .unsubscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .compose(rxAppCompatActivity.bindUntilEvent(ActivityEvent.DESTROY))
                .subscribe(subscriber);
    }

    public void getCollectedComicList(Subscriber subscriber){
        Observable.create(new Observable.OnSubscribe<List<Comic>>() {
            @Override
            public void call(Subscriber<? super List<Comic>> subscriber) {
                try {
                    List<Comic> comics = mHelper.queryCollect();
                    subscriber.onNext(comics);
                } catch (Exception e) {
                    subscriber.onError(e);
                    e.printStackTrace();
                }finally {
                    subscriber.onCompleted();
                }
            }
        }) .subscribeOn(Schedulers.io())
                .unsubscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .compose(rxAppCompatActivity.bindUntilEvent(ActivityEvent.DESTROY))
                .subscribe(subscriber);

    }

    /**
     * 插入搜索结果到数据库
     * @param title
     * @param subscriber
     */
    public void updateSearchResultToDB(final String title, Subscriber<Boolean> subscriber) {
        Observable.create(new Observable.OnSubscribe<Boolean>() {

            @Override
            public void call(Subscriber<? super Boolean> subscriber) {
                try{
                    final java.util.Date date = new java.util.Date();
                    long datetime = date.getTime();
                    DBSearchResult result = new DBSearchResult();
                    result.setTitle(title);
                    result.setSearch_time(datetime);
                    if(mHelper.insert(result)){
                        subscriber.onNext(true);
                    }else{
                        subscriber.onNext(false);
                    }
                }catch (Exception e){
                    subscriber.onError(e);
                }finally {
                    subscriber.onCompleted();
                }
            }
        }) .subscribeOn(Schedulers.io())
                .unsubscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .compose(rxAppCompatActivity.<Boolean>bindUntilEvent(ActivityEvent.DESTROY))
                .subscribe(subscriber);

    }

    public void getHistorySearch(Subscriber<List<Comic>> subscriber) {
        Observable.create(new Observable.OnSubscribe<List<Comic>>() {

            @Override
            public void call(Subscriber<? super List<Comic>> subscriber) {
                try{
                    List<DBSearchResult> results = mHelper.querySearch();
                    List<Comic> comics = DBEntityUtils.transSearchToComic(results);
                    subscriber.onNext(comics);
                }catch (Exception e){
                    subscriber.onError(e);
                }finally {
                    subscriber.onCompleted();
                }
            }
        }) .subscribeOn(Schedulers.io())
                .unsubscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .compose(rxAppCompatActivity.<List<Comic>>bindUntilEvent(ActivityEvent.DESTROY))
                .subscribe(subscriber);

    }

    public void clearSearchHistory(Subscriber subscriber) {
        Observable.create(new Observable.OnSubscribe<Boolean>() {
            @Override
            public void call(Subscriber<? super Boolean> subscriber) {
                try{
                    if(mHelper.deleteAllSearch()){
                        subscriber.onNext(true);
                    }else{
                        subscriber.onNext(false);
                    }
                }catch (Exception e){
                    subscriber.onError(e);
                }finally {
                    subscriber.onCompleted();
                }
            }
        }) .subscribeOn(Schedulers.io())
                .unsubscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .compose(rxAppCompatActivity.bindUntilEvent(ActivityEvent.DESTROY))
                .subscribe(subscriber);
    }
}
