package com.xiaoyu.HeartConsultation.widget.photo;import android.app.AlertDialog;import android.app.Dialog;import android.content.Context;import android.content.DialogInterface;import android.content.DialogInterface.OnCancelListener;import android.content.Intent;import android.database.Cursor;import android.graphics.Bitmap;import android.net.Uri;import android.os.Bundle;import android.provider.MediaStore;import android.text.TextUtils;import android.view.ContextThemeWrapper;import com.xiaoyu.HeartConsultation.R;import com.xiaoyu.HeartConsultation.background.PathManager;import com.xiaoyu.HeartConsultation.ui.ActivityBase;import com.xiaoyu.HeartConsultation.util.IntentUtils;import java.io.File;import java.util.ArrayList;import java.util.List;/** * @author yinxinya * @version 1.0 * @title:拍照或者从图库选择图片 * @description: * @company: 美丽说（北京）网络科技有限公司 * @created * @changeRecord */public class PhotoChooseImplActivity extends ActivityBase {	public final static int REQUEST_TAKE_PICTURE = 1000001;	public final static int REQUEST_TAKE_GALLARY = 1000002;	public static final int REQUEST_PREVIEW = 1000003;	public static final int REQUEST_FILTER = 100004;	public static final int REQUEST_CROP = 100005;	private String mPicPath;	private boolean mNeedCrop = false;	private String r;	private Dialog mDialog;	void onChoosePhoto(String path) {		Intent data = new Intent();		data.putExtra("path", path);		setResult(RESULT_OK, data);		finish();	}	void onChoosePhoto(String[] paths) {		Intent data = new Intent();		data.putExtra("paths", paths);		setResult(RESULT_OK, data);		finish();	}	void onChoosePhoto(Bitmap bitmap) {		Intent data = new Intent();		data.putExtra("bitmap", bitmap);		setResult(RESULT_OK, data);		finish();	}	@Override	protected void onCreate(Bundle savedInstanceState) {		super.onCreate(savedInstanceState);		if (getIntent() != null && savedInstanceState == null) {			int mode = getIntent().getIntExtra("mode", -1);			r = getIntent().getStringExtra("r");			if (mode != PreviewActivity.MODE_DEL) {				String title = getIntent().getStringExtra("title");				ArrayList<String> selectPaths = getIntent().getStringArrayListExtra("select");				int max = getIntent().getIntExtra("max", -1);				boolean needCrop = getIntent().getBooleanExtra("needcrop", false);				showPicPhotoDialog(selectPaths, max, title, needCrop);			} else {				ArrayList<String> items = getIntent().getStringArrayListExtra("items");				int position = getIntent().getIntExtra("position", -1);				Intent preview_intent = new Intent(this, PreviewActivity.class);				preview_intent.putExtra("mode", PreviewActivity.MODE_DEL);				preview_intent.putExtra("items", items);				preview_intent.putExtra("position", position);				preview_intent.putExtra("r", r);				startActivityForResult(preview_intent, PhotoChooseImplActivity.REQUEST_PREVIEW);			}		}	}	/**	 * @param selectPaths	 * @param max         -1表示使用系统自带图库选中	 * @param title	 */	public void showPicPhotoDialog(final ArrayList<String> selectPaths, final int max, String title, boolean needCrop) {		mNeedCrop = needCrop;		Context context = this;		final Context dialogContext = new ContextThemeWrapper(context, android.R.style.Theme_Light);		ArrayList<String> choices = new ArrayList<String>();		choices.add(getString(R.string.text_photogrash));		choices.add(getString(R.string.text_chooce_from_album));		final SimpleTextViewAdapter adapter = new SimpleTextViewAdapter(				dialogContext, R.layout.simple_list_item, choices);		final AlertDialog.Builder builder = new AlertDialog.Builder(dialogContext);		builder.setTitle(title);		builder.setSingleChoiceItems(adapter, -1,				new DialogInterface.OnClickListener() {					@Override					public void onClick(DialogInterface dialog, int which) {						switch (which) {						case 0:							try {								mPicPath = PathManager.getCameraPhotoPath().getAbsolutePath();								Intent intent = IntentUtils.goToCameraIntent(mPicPath);								startActivityForResult(intent, REQUEST_TAKE_PICTURE);							} catch (Exception e) {								e.printStackTrace();							}							break;						case 1:							try {								Intent intent = IntentUtils.goToAlbumIntent(selectPaths, max, "",false,PhotoChooseImplActivity.this);								startActivityForResult(intent, REQUEST_TAKE_GALLARY);							} catch (Exception e) {								e.printStackTrace();							}							break;						case 2:							break;						}					}				});		if (mDialog != null) {			mDialog.dismiss();		}		mDialog = builder.create();		mDialog.setCanceledOnTouchOutside(true);		mDialog.show();		mDialog.setOnCancelListener(new OnCancelListener() {			@Override			public void onCancel(DialogInterface dialog) {				setResult(RESULT_CANCELED);				finish();			}		});	}	@Override	protected void onActivityResult(int requestCode, int resultCode, Intent data) {		if (mDialog != null) {			mDialog.dismiss();		}		super.onActivityResult(requestCode, resultCode, data);		if (RESULT_CANCELED == resultCode) {			setResult(RESULT_CANCELED);			finish();		}		if (RESULT_OK == resultCode && REQUEST_TAKE_PICTURE == requestCode) {			scanImage(getApplicationContext(), mPicPath);			if (mNeedCrop) {// 需要剪切				startActivityForResult(getPhotoCropIntent(mPicPath), REQUEST_CROP);				return;			}			onChoosePhoto(mPicPath);		} else if (RESULT_OK == resultCode && REQUEST_TAKE_GALLARY == requestCode) {			if (data == null) {				setResult(RESULT_CANCELED);				finish();			} else {				boolean multipleChoice = data.getBooleanExtra("multipleChoice", true);				String[] paths = data.getStringArrayExtra(PhotoAlbumActivity.Key_SelectPaths);				if (paths != null) {					if (multipleChoice || paths.length == 0) {						onChoosePhoto(paths);					} else {						String imagePath = paths[0];						if (mNeedCrop) {							startActivityForResult(getPhotoCropIntent(imagePath), REQUEST_CROP);						} else {							onChoosePhoto(mPicPath);						}					}				} else {					if (data.getData() != null) {// 需要剪切						String imagePath = getImagePath(data.getData());						if (!TextUtils.isEmpty(imagePath)) {							if (mNeedCrop) {								startActivityForResult(										getPhotoCropIntent(imagePath),										REQUEST_CROP);							} else {								onChoosePhoto(imagePath);							}						}					}				}			}		} else if (RESULT_OK == resultCode && REQUEST_PREVIEW == requestCode) {			ArrayList<PreviewAdapter.PreviewItem> items = data					.getParcelableArrayListExtra("items");			List<String> paths = new ArrayList<String>();			if (items != null && !items.isEmpty()) {				for (PreviewAdapter.PreviewItem item : items) {					if (item.isSelected) {						String path = item.filterPath;						if (TextUtils.isEmpty(path)) {							path = item.originPath;						}						paths.add(path);					}				}			}			String[] picpaths = new String[paths.size()];			paths.toArray(picpaths);			onChoosePhoto(picpaths);		} else if (RESULT_OK == resultCode && REQUEST_FILTER == requestCode) {			if (data != null) {				Uri imageUri = data.getData();				if (imageUri != null) {					String path = getImagePath(imageUri);					if (!TextUtils.isEmpty(path)) {						onChoosePhoto(path);					} else {						onChoosePhoto(mPicPath);					}				} else {					onChoosePhoto(mPicPath);				}			} else {				onChoosePhoto(mPicPath);			}			//			if (data != null && data.getExtras() != null) {			//				Bundle extra = data.getExtras();			//				boolean changed = extra			//						.getBoolean(Constants.EXTRA_OUT_BITMAP_CHANGED);			//				Uri imageUri = data.getData();			//				if (changed && imageUri != null) {			//					String path = getImagePath(imageUri);			//					if (!TextUtils.isEmpty(path)) {			//						onChoosePhoto(path);			//					} else {			//						onChoosePhoto(mPicPath);			//					}			//				} else {			//					onChoosePhoto(mPicPath);			//				}			//			} else {			//				onChoosePhoto(mPicPath);			//			}		} else if (RESULT_OK == resultCode && REQUEST_CROP == requestCode) {			if (data == null) {				setResult(RESULT_CANCELED);				finish();			} else {				Bitmap bitmap = data.getParcelableExtra("data");				if (bitmap != null) {					onChoosePhoto(bitmap);				}			}		}	}	//	/**	//	 * 从图库选择图片	//	 *	//	 * @return	//	 */	//	public Intent goToAlbumIntent(ArrayList<String> selectPaths,	//			int maxCount) {	//		if (maxCount == -1) {	//			Intent intent = new Intent(Intent.ACTION_GET_CONTENT, null);	//			intent.setType("image/*");	//			intent.putExtra("return-data", false);	//			return intent;	//		}	//		Intent intent = new Intent(this, PhotoAlbumActivity.class);	//		intent.putExtra("r", r);	//		if (selectPaths == null) {	//			intent.putExtra("multipleChoice", false);// 单选	//			intent.putExtra("maxCount", 1);	//		} else {	//			intent.putExtra("multipleChoice", true);// 多选	//			intent.putExtra("selectPaths", selectPaths);	//			intent.putExtra("maxCount", maxCount);	//		}	//		return intent;	//	}	//	/**	//	 * 打开滤镜	//	 *	//	 * @param PATH	//	 * @return	//	 */	//	public Intent getFilterIntent(String path) {	//		Intent intent = new Intent(this, ImageFilterActivity.class);	//		intent.setData(Uri.fromFile(new File(path)));	//		intent.putExtra("r", r);	//		return intent;	//	}	/**	 * 对照片进行剪切	 *	 * @param path	 * @return	 */	public Intent getPhotoCropIntent(String path) {		Intent intent = new Intent("com.android.camera.action.CROP");		intent.setDataAndType(Uri.fromFile(new File(path)), "image/*");		intent.putExtra("crop", "true");		intent.putExtra("aspectX", 1);		intent.putExtra("aspectY", 1);		intent.putExtra("outputX", 150);		intent.putExtra("outputY", 150);		intent.putExtra("return-data", true);		return intent;	}	/**	 * 主动扫描文件 以便文件加入本地媒体库	 *	 * @param context	 * @param imagePath	 */	private void scanImage(Context context, String imagePath) {		if (!TextUtils.isEmpty(imagePath)) {			Uri data = Uri.parse("file:///" + imagePath);			sendBroadcast(new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, data));		}	}	@Override	protected void onSaveInstanceState(Bundle outState) {		if (!TextUtils.isEmpty(mPicPath)) {			outState.putString("picPath", mPicPath);		}		super.onSaveInstanceState(outState);	}	@Override	protected void onRestoreInstanceState(Bundle savedInstanceState) {		if (!TextUtils.isEmpty(savedInstanceState.getString("picPath"))) {			mPicPath = savedInstanceState.getString("picPath");		}		super.onRestoreInstanceState(savedInstanceState);	}	/**	 * 获取图片本地路径	 *	 * @param uri	 * @return	 */	public String getImagePath(Uri uri) {		String path = null;		if (uri == null) {			return path;		}		if ("content".equals(uri.getScheme())) {			Cursor cursor = getContentResolver().query(uri, null, null, null,					null);			cursor.moveToFirst();			String document_id = cursor.getString(0);			document_id = document_id					.substring(document_id.lastIndexOf(":") + 1);			cursor.close();			cursor = getContentResolver()					.query(MediaStore.Images.Media.EXTERNAL_CONTENT_URI,							null, MediaStore.Images.Media._ID + " = ? ",							new String[] { document_id }, null);			cursor.moveToFirst();			path = cursor.getString(cursor					.getColumnIndex(MediaStore.Images.Media.DATA));			cursor.close();		} else if ("file".equals(uri.getScheme())) {			path = uri.getPath();		}		return path;	}	@Override	protected void getViews() {	}	@Override	protected void initViews() {	}	@Override	protected void setListeners() {	}}