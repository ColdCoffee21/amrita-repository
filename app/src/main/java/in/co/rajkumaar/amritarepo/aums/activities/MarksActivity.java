/*
 * MIT License
 *
 * Copyright (c) 2018  RAJKUMAR S
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package in.co.rajkumaar.amritarepo.aums.activities;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.AsyncHttpResponseHandler;
import com.loopj.android.http.RequestParams;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.util.ArrayList;
import java.util.List;

import cz.msebera.android.httpclient.Header;
import in.co.rajkumaar.amritarepo.R;
import in.co.rajkumaar.amritarepo.aums.helpers.UserData;


public class MarksActivity extends AppCompatActivity {

    private RecyclerView recyclerView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_marks);
        recyclerView = (RecyclerView) findViewById(R.id.list);
        UserData.refIndex = 1;

        final LinearLayoutManager layoutParams = new LinearLayoutManager(this);
        recyclerView.setLayoutManager(layoutParams);
        getMarks(UserData.client, getIntent().getStringExtra("sem"));

    }

    void getMarks(final AsyncHttpClient client, final String sem) {
        RequestParams params = new RequestParams();
        params.put("action", "UMS-EVAL_STUDMARKVIEW_INIT_SCREEN");
        params.put("isMenu", "true");
        client.get(UserData.domain + "/aums/Jsp/Marks/ViewPublishedMark.jsp", params, new AsyncHttpResponseHandler() {
            @Override
            public void onSuccess(int statusCode, Header[] headers, byte[] responseBody) {
                RequestParams params = new RequestParams();
                params.put("htmlPageTopContainer_selectStep", sem);
                params.put("Page_refIndex_hidden", UserData.refIndex++);
                params.put("htmlPageTopContainer_status", "");
                params.put("htmlPageTopContainer_action", "UMS-EVAL_STUDMARKVIEW_SELSEM_SCREEN");
                params.put("htmlPageTopContainer_notify", "I");

                client.post(UserData.domain + "/aums/Jsp/Marks/ViewPublishedMark.jsp?action=UMS-EVAL_STUDMARKVIEW_INIT_SCREEN&isMenu=true", params, new AsyncHttpResponseHandler() {
                    @Override
                    public void onSuccess(int statusCode, Header[] headers, byte[] responseBody) {
                        ArrayList<String> subjects = new ArrayList<>();
                        List<CourseMarkData> markDataList = new ArrayList<>();
                        Document doc = Jsoup.parse(new String(responseBody));

                        try {
                            Element table = doc.select("table[width=75%]").first();

                            if (table == null) {
                                Toast.makeText(MarksActivity.this, "Marks unavailable for this semester!", Toast.LENGTH_LONG).show();
                                finish();
                            }

                            Elements rows = table.select("tr");
                            Elements headerRowCells = rows.get(0).select("td");

                            for (int i = 3; i < headerRowCells.size(); i++) {
                                Element cell = headerRowCells.get(i);
                                if (cell.text().trim().length() > 0) {
                                    subjects.add(cell.text().trim());
                                }
                            }

                            if (subjects.size() == 0) {
                                Toast.makeText(MarksActivity.this, "Marks unavailable for this semester!", Toast.LENGTH_LONG).show();
                                finish();
                            }

                            for (int i = 1; i < rows.size(); i++) {
                                boolean hasMarks = false;
                                Elements cells = rows.get(i).select("td");
                                String exam = cells.get(0).text();
                                int k = 0;
                                for (int j = 3; j < cells.size(); j++) {
                                    Element cell = cells.get(j);
                                    String mark = cell.text();
                                    if (isNumeric(mark)) {
                                        hasMarks = true;
                                    }
                                    k++;
                                }
                                if (hasMarks) {
                                    markDataList.add(new CourseMarkData(exam));
                                    k = 0;
                                    for (int j = 3; j < cells.size(); j++) {
                                        Element cell = cells.get(j);
                                        String mark = cell.text();
                                        if (isNumeric(mark)) {
                                            markDataList.add(new CourseMarkData(subjects.get(k), mark, exam));
                                        }
                                        k++;
                                    }
                                }
                            }

                            if (markDataList.size() > 0) {
                                Log.e("SIze", markDataList.size() + "");
                                setupList(markDataList);
                            } else {
                                Toast.makeText(MarksActivity.this, "Marks unavailable for this semester!", Toast.LENGTH_LONG).show();
                                finish();
                            }

                        } catch (Exception e) {
                            Toast.makeText(MarksActivity.this, "Site's structure has changed. Please wait until I catch up", Toast.LENGTH_LONG).show();
                            finish();
                        }
                    }

                    @Override
                    public void onFailure(int statusCode, Header[] headers, byte[] responseBody, Throwable error) {
                        Toast.makeText(MarksActivity.this, "An error occurred while connecting to server", Toast.LENGTH_LONG).show();
                        finish();
                    }
                });
            }

            @Override
            public void onFailure(int statusCode, Header[] headers, byte[] responseBody, Throwable error) {
                Toast.makeText(MarksActivity.this, "An error occurred while connecting to server", Toast.LENGTH_LONG).show();
                finish();
            }
        });
    }

    public void setupList(List<CourseMarkData> markDataList) {
        MarksAdapter adapter = new MarksAdapter(markDataList);
        recyclerView.setAdapter(adapter);
        findViewById(R.id.progressBar).setVisibility(View.GONE);
        recyclerView.setVisibility(View.VISIBLE);
    }

    private boolean isNumeric(String str) {
        return str.matches("-?\\d+(\\.\\d+)?");
    }

    public class MarksAdapter extends RecyclerView.Adapter<MarksAdapter.ViewHolder> {

        private List<CourseMarkData> courseMarkDataList;
        private int HEADER = 1;
        private int ITEM = 2;

        MarksAdapter(List<CourseMarkData> courseMarkDataList) {
            this.courseMarkDataList = courseMarkDataList;
        }

        @Override
        public int getItemViewType(int position) {
            if (courseMarkDataList.get(position).mark == null) {
                return HEADER;
            }
            return ITEM;
        }

        @Override
        public MarksAdapter.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View v;

            if (viewType == HEADER) {
                v = LayoutInflater.from(parent.getContext()).inflate(R.layout.aums_marks_title, parent, false);
            } else {
                v = LayoutInflater.from(parent.getContext()).inflate(R.layout.aums_mark_item, parent, false);
            }

            return new ViewHolder(v);
        }

        @Override
        public void onBindViewHolder(ViewHolder holder, int position) {
            CourseMarkData courseMarkData = courseMarkDataList.get(position);

            if (courseMarkData.mark == null) {
                holder.sectionHeader.setText(courseMarkData.exam);
            } else {
                holder.name.setText(courseMarkData.courseCode);
                holder.value.setText(courseMarkData.mark);
            }

        }

        @Override
        public int getItemCount() {
            return courseMarkDataList.size();
        }

        class ViewHolder extends RecyclerView.ViewHolder {

            public TextView name;
            public TextView value;
            public View indicator;

            TextView sectionHeader;

            ViewHolder(View v) {
                super(v);
                try {
                    name = (TextView) v.findViewById(R.id.name);
                    value = (TextView) v.findViewById(R.id.value);
                    indicator = (View) v.findViewById(R.id.indicator);
                } catch (Exception ignored) {
                }

                try {
                    sectionHeader = (TextView) v.findViewById(R.id.section_header);
                } catch (Exception ignored) {
                }
            }

        }
    }

    public class CourseMarkData {

        String courseCode = null;
        String mark = null;
        String exam = null;


        CourseMarkData(String exam) {
            this.exam = exam;
        }

        CourseMarkData(String courseCode, String mark, String exam) {
            this.courseCode = courseCode;
            this.mark = mark;
            this.exam = exam;
        }
    }
}
