package com.widiarifki.findtutor.fragment;

import android.Manifest;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.annotation.Nullable;
import android.support.annotation.RequiresApi;
import android.support.v4.app.Fragment;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AlertDialog;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.RelativeLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.squareup.picasso.Picasso;
import com.theartofdev.edmodo.cropper.CropImage;
import com.theartofdev.edmodo.cropper.CropImageView;
import com.widiarifki.findtutor.R;
import com.widiarifki.findtutor.adapter.EducationListAdapter;
import com.widiarifki.findtutor.app.App;
import com.widiarifki.findtutor.app.SessionManager;
import com.widiarifki.findtutor.helper.CircleTransform;
import com.widiarifki.findtutor.helper.RunnableDialogMessage;
import com.widiarifki.findtutor.model.Education;
import com.widiarifki.findtutor.model.SubjectTopic;
import com.widiarifki.findtutor.model.User;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

/**
 * Created by widiarifki on 28/05/2017.
 */

public class SettingProfileFragment extends Fragment {

    private Context mContext;
    private Activity mContextActivity;
    private String dialogTitle = "Submit Data Gagal";

    SessionManager mSession;
    User mUserLogin;

    // UI comp.
    LinearLayout mFormLayout;
    EditText mInputName;
    RadioGroup mRgrupGender;
    EditText mInputEmail;
    EditText mInputPhone;
    EditText mInputBio;
    ImageView mImgUserPhoto;
    Button mBtnTakePhoto;
    Button mBtnPickPhoto;
    ListView mListviewEducation;
    Button mBtnSave;
    Button mBtnAddEdu;
    Button mBtnChooseSubject;
    private RelativeLayout mFieldBio;
    private RelativeLayout mFieldEducation;
    ProgressDialog mProgressDialog;

    int mSchoolLevelListLn;
    String[] mSchoolLevelList;
    HashMap<Integer,String> mSchoolLevelMap;

    List<Education> mEducations;
    EducationListAdapter mEduListAdapter;
    ArrayList<SubjectTopic> mSubjects;
    private Uri mPhotoUri;
    private Uri mSavedPhotoUri;

    static final int REQUEST_IMAGE_CAPTURE = 1;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.appbar_menu, menu);
        menu.findItem(R.id.action_save).setVisible(true);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_save) {
            saveChanges();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        mContext = container.getContext();
        mContextActivity = (Activity)mContext;
        View view = inflater.inflate(R.layout.fragment_setting_profile, container, false);

        mSession = new SessionManager(mContext);
        mUserLogin = mSession.getUserDetail();

        mProgressDialog = new ProgressDialog(mContext);
        if(mUserLogin.getIsTutor() == 1)
            fetchRefData();

        // Bind UI comp.
        mFormLayout = (LinearLayout) view.findViewById(R.id.form_layout);
        mFieldBio = (RelativeLayout) view.findViewById(R.id.fieldBio);
        mFieldEducation = (RelativeLayout) view.findViewById(R.id.fieldEducation);
        mImgUserPhoto = (ImageView) view.findViewById(R.id.imgv_profile_photo);
        mInputName = (EditText) view.findViewById(R.id.input_name);
        mRgrupGender = (RadioGroup) view.findViewById(R.id.rgrup_gender);
        //mInputEmail = (EditText) view.findViewById(R.id.input_email);
        mInputPhone = (EditText) view.findViewById(R.id.input_phone);
        mInputBio = (EditText) view.findViewById(R.id.input_bio);
        mInputBio.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    // Do whatever you want here
                    return true;
                }
                return false;
            }
        });
        mBtnTakePhoto = (Button) view.findViewById(R.id.btn_take_photo);
        mBtnTakePhoto.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                takePicture();
            }
        });
        mBtnPickPhoto = (Button) view.findViewById(R.id.btn_pick_photo);
        mBtnPickPhoto.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                pickPicture();
            }
        });
        mEducations = new ArrayList<Education>();
        mEduListAdapter = new EducationListAdapter(mContext, mEducations);
        mListviewEducation = (ListView) view.findViewById(R.id.list_education);
        mListviewEducation.setAdapter(mEduListAdapter);
        mListviewEducation.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, final int position, long id) {
                AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(mContext);
                final String[] actions = {"Edit", "Hapus"};
                dialogBuilder.setItems(actions, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        String selectedAction = actions[which];
                        if(selectedAction == "Edit"){
                            actionUpdateEducation(false, position);
                        }
                        else if(selectedAction == "Hapus"){
                            dialog.dismiss();
                            AlertDialog.Builder dialogConfBuilder = new AlertDialog.Builder(mContext);
                            dialogConfBuilder.setTitle(getString(R.string.delete_confirmation));
                            dialogConfBuilder.setMessage("Apakah anda yakin akan menghapus data ini?");
                            dialogConfBuilder.setCancelable(true);
                            dialogConfBuilder.setNegativeButton(mContext.getString(R.string.action_cancel), new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    dialog.cancel();
                                }
                            });
                            dialogConfBuilder.setPositiveButton(mContext.getString(android.R.string.ok), new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    mEducations.remove(position);
                                    mEduListAdapter.notifyDataSetChanged();
                                    App.setListViewHeightBasedOnChildren(mListviewEducation);

                                    dialog.dismiss();
                                }
                            });
                            AlertDialog alertDialogConf = dialogConfBuilder.create();
                            alertDialogConf.show();
                        }
                    }
                });
                AlertDialog alertDialog = dialogBuilder.create();
                alertDialog.show();
            }
        });

        mBtnAddEdu = (Button) view.findViewById(R.id.btn_add_edu);
        mBtnAddEdu.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                actionUpdateEducation(true, 0);
            }
        });

        adjustWithUserType();
        bindInitialData();

        return view;
    }

    void pickPicture(){
        CropImage.startPickImageActivity(mContext, this);
    }

    private void adjustWithUserType() {
        if(mUserLogin.getIsTutor() == 0){
            mFieldBio.setVisibility(View.GONE);
            mFieldEducation.setVisibility(View.GONE);
        }else{
            mFieldBio.setVisibility(View.VISIBLE);
            mFieldEducation.setVisibility(View.VISIBLE);
        }
    }

    void bindInitialData(){
        mUserLogin = mSession.getUserDetail();
        int gender = mUserLogin.getGender();
        Picasso.with(mContext).load(App.URL_PATH_PHOTO + mUserLogin.getPhotoUrl())
                .transform(new CircleTransform())
                .placeholder(R.drawable.ic_person_black_24dp)
                .error(R.drawable.ic_broken_image_black_24dp)
                .into(mImgUserPhoto);
        mInputName.setText(mUserLogin.getName());
        RadioButton selectedRadio = null;
        if(gender == 1) {
            selectedRadio = (RadioButton) mRgrupGender.findViewById(R.id.radio_opt_male);
        }
        else if(gender == 2) {
            selectedRadio = (RadioButton) mRgrupGender.findViewById(R.id.radio_opt_female);
        }
        selectedRadio.setChecked(true);
        //mInputEmail.setText(mUserLogin.getEmail());
        mInputPhone.setText(mUserLogin.getPhone());
        mInputBio.setText(mUserLogin.getBio());
        mEducations.clear();
        mEduListAdapter.notifyDataSetChanged();
        if(mUserLogin.getEducations()!=null) {
            if (mUserLogin.getEducations().size() > 0) {
                mEducations.addAll(mUserLogin.getEducations());
                mEduListAdapter.notifyDataSetChanged();
                App.setListViewHeightBasedOnChildren(mListviewEducation);
            }
        }
    }

    @Override
    public void onResume() {
        super.onResume();
    }

    private void fetchRefData() {
        mProgressDialog.setMessage("Tunggu sebentar...");
        if(!mProgressDialog.isShowing()) mProgressDialog.show();

        OkHttpClient client = new OkHttpClient();
        Request httpRequest = new Request.Builder()
                .url(App.URL_GET_SCHOOL_LEVEL)
                .build();
        Call httpCall = client.newCall(httpRequest);
        httpCall.enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                new RunnableDialogMessage(mContext, "Gagal mendapatkan data tingkat pendidikan", e.getMessage(), mProgressDialog);
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                String json = response.body().string();
                if(response.isSuccessful() && response.code() == 200){
                    try {
                        JSONArray dataJson = new JSONArray(json);
                        mSchoolLevelListLn = dataJson.length();
                        mSchoolLevelList = new String[mSchoolLevelListLn];
                        mSchoolLevelMap = new HashMap<Integer, String>();
                        for (int i = 0; i < mSchoolLevelListLn; i++) {
                            JSONObject dataObj = dataJson.getJSONObject(i);
                            mSchoolLevelList[i] = dataObj.getString("REF_DESC");
                            mSchoolLevelMap.put(i, dataObj.getString("REF_CODE"));
                        }
                        /** Hide progress bae **/
                        mContextActivity.runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                if(mProgressDialog.isShowing()) mProgressDialog.dismiss();
                            }
                        });
                    } catch (JSONException e) {
                        new RunnableDialogMessage(mContext, "Gagal mendapatkan data tingkat pendidikan", e.getMessage(), mProgressDialog);
                    }
                }else{
                    new RunnableDialogMessage(mContext, "Gagal mendapatkan data tingkat pendidikan", response.message(), mProgressDialog);
                }
            }
        });
    }

    private void takePicture() {
        if (mContext.getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA)) {
            Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            //resolveActivity(), returns the first activity component that can handle the intent
            if (takePictureIntent.resolveActivity(mContext.getPackageManager()) != null) {
                // Create the File where the photo should go
                File photoFile = null;
                try {
                    photoFile = createImageFile();
                } catch (IOException ex) {
                    // Error occurred while creating the File
                    ex.printStackTrace();
                }

                // Continue only if the File was successfully created
                if (photoFile != null) {
                    mPhotoUri = FileProvider.getUriForFile(mContext, mContext.getPackageName(), photoFile);
                    takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, mPhotoUri);
                    startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE);
                }
            }else{
                Toast.makeText(mContextActivity.getApplication(), "Aplikasi kamera tidak ditemukan", Toast.LENGTH_LONG).show();
            }
        }else{
            Toast.makeText(mContextActivity.getApplication(), "Camera not supported", Toast.LENGTH_LONG).show();
        }
    }

    private File createImageFile() throws IOException {
        // Create an image file name
        String imageFileName = mUserLogin.getEmail();
        File storageDir = mContext.getExternalFilesDir(Environment.DIRECTORY_PICTURES);

        File image = File.createTempFile(
                imageFileName,  /* prefix */
                ".jpg",         /* suffix */
                storageDir      /* directory */
        );

        // Save a file: path for use with ACTION_VIEW intents
        return image;
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if(requestCode == REQUEST_IMAGE_CAPTURE && resultCode == Activity.RESULT_OK) {
            // start picker to get image for cropping and then use the image in cropping activity
            CropImage.activity(mPhotoUri)
                    .setGuidelines(CropImageView.Guidelines.OFF)
                    .setCropShape(CropImageView.CropShape.OVAL)
                    .setFixAspectRatio(true)
                    .setAllowFlipping(false)
                    .setActivityTitle("Tampilan Foto")
                    .start(getContext(), this);
        }

        else if (requestCode == CropImage.PICK_IMAGE_CHOOSER_REQUEST_CODE && resultCode == Activity.RESULT_OK) {
            Uri imageUri = CropImage.getPickImageResultUri(mContext, data);
            mPhotoUri = imageUri;
            // For API >= 23 we need to check specifically that we have permissions to read external storage.
            if (CropImage.isReadExternalStoragePermissionsRequired(mContext, imageUri)) {
                // request permissions and handle the result in onRequestPermissionsResult()
                requestPermissions(new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, CropImage.PICK_IMAGE_PERMISSIONS_REQUEST_CODE);
            } else {
                // no permissions required or already grunted, can start crop image activity
                CropImage.activity(mPhotoUri)
                        .setGuidelines(CropImageView.Guidelines.OFF)
                        .setCropShape(CropImageView.CropShape.OVAL)
                        .setFixAspectRatio(true)
                        .setAllowFlipping(false)
                        .setActivityTitle("Tampilan Foto")
                        .start(getContext(), this);
            }
        }

        else if (requestCode == CropImage.CROP_IMAGE_ACTIVITY_REQUEST_CODE) {
            CropImage.ActivityResult result = CropImage.getActivityResult(data);
            if (resultCode == Activity.RESULT_OK) {
                mSavedPhotoUri = result.getUri();
                Picasso.with(mContext).load(mSavedPhotoUri)
                        .transform(new CircleTransform())
                        .into(mImgUserPhoto);
            } else if (resultCode == CropImage.CROP_IMAGE_ACTIVITY_RESULT_ERROR_CODE) {
                Exception error = result.getError();
            }
        }
    }

    private void actionUpdateEducation(final boolean isNewData, final int editedItemPos){
        View currentFocus = getActivity().getCurrentFocus();
        if (currentFocus != null) currentFocus.clearFocus();

        App.hideSoftKeyboard(mContext);

        final View dialogView = LayoutInflater.from(mContext).inflate(R.layout.dialog_layout_add_edu, null);
        final EditText inputYear = (EditText) dialogView.findViewById(R.id.input_year);
        final EditText inputSchoolName = (EditText) dialogView.findViewById(R.id.input_school_name);
        final EditText inputSchoolDept = (EditText) dialogView.findViewById(R.id.input_department);
        final Spinner ddSchoolLevel = (Spinner) dialogView.findViewById(R.id.spinner_school_level);
        ArrayAdapter<String> listSchoolLevel = new ArrayAdapter<String>(mContext, android.R.layout.simple_spinner_item, mSchoolLevelList);
        listSchoolLevel.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        ddSchoolLevel.setAdapter(listSchoolLevel);

        if(!isNewData){
            // Bind data to form
            Education editedData = mEducations.get(editedItemPos);
            inputYear.setText(editedData.getYearGraduate());
            inputSchoolName.setText(editedData.getSchoolName());
            inputSchoolDept.setText(editedData.getDepartment());
            ddSchoolLevel.setSelection(listSchoolLevel.getPosition(editedData.getSchoolLevelText()));
        }

        final AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(mContext);
        dialogBuilder.setTitle(mContext.getString(R.string.action_add_education));
        dialogBuilder.setView(dialogView);
        dialogBuilder.setCancelable(true);
        dialogBuilder.setNegativeButton(mContext.getString(R.string.action_cancel), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
            }
        });
        dialogBuilder.setPositiveButton(mContext.getString(android.R.string.ok), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                Education education = new Education();
                education.setIdUser(mUserLogin.getId());
                education.setYearGraduate(inputYear.getText().toString());
                int selectedIndex = ddSchoolLevel.getSelectedItemPosition();
                education.setSchoolLevel(Integer.parseInt(mSchoolLevelMap.get(selectedIndex)));
                education.setSchoolLevelText(ddSchoolLevel.getSelectedItem().toString());
                education.setSchoolName(inputSchoolName.getText().toString());
                education.setDepartment(inputSchoolDept.getText().toString());

                if(isNewData) {
                    mEduListAdapter.addToList(education);
                    App.setListViewHeightBasedOnChildren(mListviewEducation);
                }else{
                    mEducations.set(editedItemPos, education);
                    mEduListAdapter.notifyDataSetChanged();
                }

                mListviewEducation.requestFocus();

                App.hideSoftKeyboard(mContext);
            }
        });
        AlertDialog alertDialog = dialogBuilder.create();
        if(!alertDialog.isShowing()) alertDialog.show();
    }

    void saveChanges(){
        // Store values
        String name = mInputName.getText().toString();
        //String email = mInputEmail.getText().toString();
        String phone = mInputPhone.getText().toString();
        String bio = mInputBio.getText().toString();
        int selectedGender = mRgrupGender.getCheckedRadioButtonId();
        int gender = 0;
        if(selectedGender == R.id.radio_opt_male) gender = 1;
        else if(selectedGender == R.id.radio_opt_female) gender = 2;

        // Pass validation
        mProgressDialog.setCancelable(false);
        mProgressDialog.setMessage("Menyimpan perubahan...");
        if(!mProgressDialog.isShowing()) mProgressDialog.show();

        MediaType MEDIA_TYPE_JSON = MediaType.parse("application/json; charset=utf-8");
        MediaType MEDIA_TYPE_JPG = MediaType.parse("image/jpg");
        // convert your list to json
        String jsonEducationList = new Gson().toJson(mEducations);
        MultipartBody.Builder formBodyBuilder = new MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("id_user", mUserLogin.getId()+"")
                //.addFormDataPart("email", email)
                .addFormDataPart("name", name)
                .addFormDataPart("gender", gender+"")
                .addFormDataPart("phone", phone)
                .addFormDataPart("bio", bio)
                .addFormDataPart("educations", null, RequestBody.create(MEDIA_TYPE_JSON, jsonEducationList));
        if(mSavedPhotoUri != null)
                formBodyBuilder.addFormDataPart("user_photo", mUserLogin.getId()+".jpg", RequestBody.create(MEDIA_TYPE_JPG, new File(mSavedPhotoUri.getPath())) );

        RequestBody formBody = formBodyBuilder.build();

        OkHttpClient httpClient = new OkHttpClient();

        Request httpRequest = new Request.Builder()
                .url(App.URL_EDIT_PROFILE)
                .post(formBody)
                .build();

        Call httpCall = httpClient.newCall(httpRequest);
        httpCall.enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                // alert user
                getActivity().runOnUiThread(new RunnableDialogMessage(mContext, dialogTitle, String.valueOf(e), mProgressDialog));
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                String responseStr = response.body().string();
                if(response.isSuccessful() && response.code() == 200){
                    try {
                        JSONObject responseObj = new JSONObject(responseStr);
                        int status = responseObj.getInt("success");
                        //Log.v(TAG, status+"");
                        if(status == 1){
                            // Retrieve user data from http response
                            String userData = responseObj.getString("data");
                            JSONObject objUserData = new JSONObject(userData);
                            // Store in Session w/ User object
                            mUserLogin.setName(objUserData.getString(mSession.KEY_NAME));
                            mUserLogin.setGender(objUserData.getInt(mSession.KEY_GENDER));
                            //mUserLogin.setEmail(objUserData.getString(mSession.KEY_EMAIL));
                            mUserLogin.setPhone(objUserData.getString(mSession.KEY_PHONE));
                            mUserLogin.setBio(objUserData.getString(mSession.KEY_BIO));
                            Type type = new TypeToken<List<Education>>(){}.getType();
                            List<Education> educationList = new Gson().fromJson(objUserData.getString(mSession.KEY_EDUCATIONS), type);
                            mUserLogin.setEducations(educationList);
                            mSession.updateSession(mUserLogin);

                            getActivity().runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    if(mProgressDialog.isShowing()) mProgressDialog.dismiss();
                                    Toast.makeText(mContext, "Simpan profil berhasil", Toast.LENGTH_SHORT).show();
                                }
                            });
                        }else{
                            String message = responseObj.getString("error_msg");
                            // alert user
                            getActivity().runOnUiThread(new RunnableDialogMessage(mContext, dialogTitle, message, mProgressDialog));
                        }
                    } catch (JSONException e) {
                        // alert user
                        getActivity().runOnUiThread(new RunnableDialogMessage(mContext, dialogTitle, e.getMessage(), mProgressDialog));
                    }
                }else{
                    // alert user
                    getActivity().runOnUiThread(new RunnableDialogMessage(mContext, dialogTitle, response.message(), mProgressDialog));
                }
            }
        });
    }
}
