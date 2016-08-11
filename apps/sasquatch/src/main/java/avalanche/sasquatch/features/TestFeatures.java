package avalanche.sasquatch.features;

import android.app.Activity;
import android.content.Intent;
import android.view.View;
import android.widget.AdapterView;

import java.util.ArrayList;
import java.util.List;

import avalanche.errors.ErrorReporting;
import avalanche.sasquatch.R;
import avalanche.sasquatch.activities.DeviceInfoActivity;
import avalanche.sasquatch.activities.DummyActivity;
import avalanche.sasquatch.activities.EventActivity;
import avalanche.sasquatch.activities.PageActivity;

public final class TestFeatures {
    private static List<TestFeatureModel> sTestFeatureModel;
    private static Activity sParentActivity;

    public static void initialize(Activity parentActivity) {
        sTestFeatureModel = new ArrayList<>();
        sParentActivity = parentActivity;
        sTestFeatureModel.add(new TestFeatureModel(R.string.title_crash, R.string.description_crash, new View.OnClickListener() {

            @Override
            @SuppressWarnings({"ConstantConditions", "ResultOfMethodCallIgnored"})
            public void onClick(View v) {

                /* Make the app crash on purpose for testing report. */
                ErrorReporting.generateTestCrash();
            }
        }));
        sTestFeatureModel.add(new TestFeatureModel(R.string.title_device_info, R.string.description_device_info, DeviceInfoActivity.class));
        sTestFeatureModel.add(new TestFeatureModel(R.string.title_event, R.string.description_event, EventActivity.class));
        sTestFeatureModel.add(new TestFeatureModel(R.string.title_page, R.string.description_page, PageActivity.class));
        sTestFeatureModel.add(new TestFeatureModel(R.string.title_generate_page_log, R.string.description_generate_page_log, DummyActivity.class));
    }

    public static List<TestFeatureModel> getAvailableControls() {
        return sTestFeatureModel;
    }

    public static AdapterView.OnItemClickListener getOnItemClickListener() {
        return new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                TestFeatureModel model = (TestFeatureModel) parent.getItemAtPosition(position);
                model.mOnClickListener.onClick(view);
            }
        };
    }

    private static View.OnClickListener getDefaultOnClickListener(final Class<? extends Activity> clazz) {
        return new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sParentActivity.startActivity(new Intent(sParentActivity, clazz));
            }
        };
    }

    public static class TestFeatureModel {
        private final String mTitle;
        private final String mDescription;
        private final View.OnClickListener mOnClickListener;

        public TestFeatureModel(int title, int description, Class<? extends Activity> clazz) {
            this(title, description, getDefaultOnClickListener(clazz));
        }

        public TestFeatureModel(int title, int description, View.OnClickListener listener) {
            this.mTitle = title > 0 ? sParentActivity.getResources().getString(title) : "";
            this.mDescription = description > 0 ? sParentActivity.getResources().getString(description) : "";
            this.mOnClickListener = listener;
        }

        public String getTitle() {
            return mTitle;
        }

        public String getDescription() {
            return mDescription;
        }
    }
}
