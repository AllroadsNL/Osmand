/**
 *
 */
package net.osmand.plus.activities;

import java.io.File;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

import android.content.Intent;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;
import net.osmand.plus.GpxSelectionHelper;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.OsmandSettings;
import net.osmand.plus.R;
import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TabHost;
import android.widget.TabHost.TabSpec;
import android.widget.TabWidget;
import android.widget.TextView;
import net.osmand.plus.views.controls.PagerSlidingTabStrip;

/**
 *
 */
public class FavoritesActivity extends TabActivity {

	private static final String FAVOURITES_INFO = "FAVOURITES_INFO";
	private static final String TRACKS = "TRACKS";
	private static final String SELECTED_TRACK = "SELECTED_TRACK";
	public static int FAVORITES_TAB = 0;
	public static int GPX_TAB = 1;
	public static int SELECTED_GPX_TAB = 2;
	public static String TAB_PARAM = "TAB_PARAM";
	private TabSpec selectedTrack;
	protected List<WeakReference<Fragment>> fragList = new ArrayList<WeakReference<Fragment>>();

	@Override
	public void onCreate(Bundle icicle) {
        //This has to be called before setContentView and you must use the
        //class in com.actionbarsherlock.view and NOT android.view
		((OsmandApplication) getApplication()).applyTheme(this);
		super.onCreate(icicle);
		setSupportProgressBarIndeterminateVisibility(false);
		getSupportActionBar().setTitle(R.string.favorites_Button);
		getSupportActionBar().setElevation(0);

		File[] lf = ((OsmandApplication) getApplication()).getAppPath(TRACKS).listFiles();
		boolean hasGpx =  false;
		if(lf != null) {
			for(File t : lf) {
				if(t.isDirectory() || (t.getName().toLowerCase().endsWith(".gpx"))) {
					hasGpx = true;
					break;
				}
			}
		}
		
		if(!hasGpx) {
			setContentView(R.layout.search_activity_single);
			getSupportFragmentManager().beginTransaction().add(R.id.layout, new FavoritesTreeFragment()).commit();
		} else {
			setContentView(R.layout.tab_content);

			PagerSlidingTabStrip mSlidingTabLayout = (PagerSlidingTabStrip) findViewById(R.id.sliding_tabs);
			OsmandSettings settings = ((OsmandApplication) getApplication()).getSettings();
			Integer tab = settings.FAVORITES_TAB.get();
			ViewPager mViewPager = (ViewPager) findViewById(R.id.pager);

			List<TabItem> mTabs = new ArrayList<TabItem>();
			mTabs.add(getTabIndicator(R.string.my_favorites, FavoritesTreeFragment.class));
			mTabs.add(getTabIndicator(R.string.my_tracks, AvailableGPXFragment.class));
			mTabs.add(getTabIndicator(R.string.selected_track, SelectedGPXFragment.class));

			setViewPagerAdapter(mViewPager, mTabs);
			mSlidingTabLayout.setViewPager(mViewPager);


			Intent intent = getIntent();
			if(intent != null) {
				int tt = intent.getIntExtra(TAB_PARAM, -1);
				if(tt >= 0) {
					mViewPager.setCurrentItem(tt);
				}
			} else {
				mViewPager.setCurrentItem(tab);
			}
			updateSelectedTracks();
		}
		//setupHomeButton();
	}

	@Override
	public void onAttachFragment(Fragment fragment) {
		fragList.add(new WeakReference<Fragment>(fragment));
	}

	@Override
	protected void onResume() {
		super.onResume();
		((OsmandApplication) getApplication()).getSelectedGpxHelper().setUiListener(FavoritesActivity.class,new Runnable() {
			
			@Override
			public void run() {
				updateSelectedTracks();
			}
		});
	}
	
	@Override
	protected void onPause() {
		super.onPause();
		((OsmandApplication) getApplication()).getSelectedGpxHelper().setUiListener(FavoritesActivity.class, null);
	}

	public void updateSelectedTracks() {
		for (WeakReference<Fragment> ref : fragList) {
			Fragment f = ref.get();
			if (f instanceof SelectedGPXFragment && !f.isDetached()) {
				GpxSelectionHelper gpx = ((OsmandApplication) getApplication()).getSelectedGpxHelper();
				String vl = getString(R.string.selected_track);
				if (gpx.isShowingAnyGpxFiles()) {
					vl += " (" + gpx.getSelectedGPXFiles().size()
							+ ")";
				} else {
					vl += " (0)";
				}
				try{
					((TextView)f.getView().findViewById(android.R.id.title)).setText(vl);
				} catch (NullPointerException e) {
				}
			}
		}
	}

	public Toolbar getClearToolbar(boolean visible) {
		final Toolbar tb = (Toolbar) findViewById(R.id.bottomControls);
		tb.setTitle(null);
		tb.getMenu().clear();
		tb.setVisibility(visible? View.VISIBLE : View.GONE);
		return tb;
	}

	public void setToolbarVisibility(boolean visible){
		findViewById(R.id.bottomControls).setVisibility(visible? View.VISIBLE : View.GONE);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		int itemId = item.getItemId();
		switch (itemId) {
		case android.R.id.home:
			finish();
			return true;

		}
		return false;
	}

    /**
     * This is a helper class that implements the management of tabs and all
     * details of connecting a ViewPager with associated TabHost.  It relies on a
     * trick.  Normally a tab host has a simple API for supplying a View or
     * Intent that each tab will show.  This is not sufficient for switching
     * between pages.  So instead we make the content part of the tab host
     * 0dp high (it is not shown) and the TabsAdapter supplies its own dummy
     * view to show as the tab content.  It listens to changes in tabs, and takes
     * care of switch to the correct paged in the ViewPager whenever the selected
     * tab changes.
     */
    public static class TabsAdapter extends FragmentPagerAdapter
            implements TabHost.OnTabChangeListener, ViewPager.OnPageChangeListener {
        private final Context mContext;
        private final TabHost mTabHost;
        private final ViewPager mViewPager;
        private final ArrayList<TabInfo> mTabs = new ArrayList<TabInfo>();
		private OsmandSettings osmSettings;
		private boolean favorites;

        static final class TabInfo {
            private final String tag;
            private Class<?> clss;
            private Bundle args;

            TabInfo(String _tag, Class<?> _class, Bundle _args) {
                tag = _tag;
                clss = _class;
                args = _args;
            }
        }

        static class DummyTabFactory implements TabHost.TabContentFactory {
            private final Context mContext;

            public DummyTabFactory(Context context) {
                mContext = context;
            }

            @Override
            public View createTabContent(String tag) {
                View v = new View(mContext);
                v.setMinimumWidth(0);
                v.setMinimumHeight(0);
                return v;
            }
        }

        public TabsAdapter(FragmentActivity activity, TabHost tabHost,ViewPager pager, OsmandSettings settings, boolean favorites) {
            super(activity.getSupportFragmentManager());
			this.favorites = favorites;
            mContext = activity;
            mTabHost = tabHost;
            mViewPager = pager;
			osmSettings = settings;
            mTabHost.setOnTabChangedListener(this);
            mViewPager.setAdapter(this);
            mViewPager.setOnPageChangeListener(this);
        }

        public TabSpec addTab(TabHost.TabSpec tabSpec, Class<?> clss, Bundle args) {
            tabSpec.setContent(new DummyTabFactory(mContext));
            String tag = tabSpec.getTag();

            TabInfo info = new TabInfo(tag, clss, args);
            mTabs.add(info);
            mTabHost.addTab(tabSpec);
            notifyDataSetChanged();
            return tabSpec;
        }
        

        @Override
        public int getCount() {
            return mTabs.size();
        }
        
        @Override
        public Fragment getItem(int position) {
            TabInfo info = mTabs.get(position);
            return Fragment.instantiate(mContext, info.clss.getName(), info.args);
        }

        @Override
        public void onTabChanged(String tabId) {
            int position = mTabHost.getCurrentTab();
			if (favorites){
				osmSettings.FAVORITES_TAB.set(position);
			}
            mViewPager.setCurrentItem(position);
        }

        @Override
        public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
        }

        @Override
        public void onPageSelected(int position) {
            // Unfortunately when TabHost changes the current tab, it kindly
            // also takes care of putting focus on it when not in touch mode.
            // The jerk.
            // This hack tries to prevent this from pulling focus out of our
            // ViewPager.
            TabWidget widget = mTabHost.getTabWidget();
            int oldFocusability = widget.getDescendantFocusability();
            widget.setDescendantFocusability(ViewGroup.FOCUS_BLOCK_DESCENDANTS);
            mTabHost.setCurrentTab(position);
            widget.setDescendantFocusability(oldFocusability);
        }

        @Override
        public void onPageScrollStateChanged(int state) {
        }
    }

}

