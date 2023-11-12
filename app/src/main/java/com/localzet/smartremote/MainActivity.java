package com.localzet.smartremote;

import android.os.Bundle;
import android.os.Handler;
import android.text.Html;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.CheckBox;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.material.textfield.TextInputEditText;

public class MainActivity extends AppCompatActivity {
    // Поле ввода для IP-адреса
    private TextInputEditText ipInputField;
    // Текстовое поле для ссылки на лог тестового бэкенда
    private TextView webLogLink;
    // Текстовое поле для лога
    private TextView logTextField;
    // Переключатель для тестирования бэкенда
    private CheckBox backendTestToggle;
    // Переключатель для отображения лога
    private CheckBox logDisplayToggle;
    // Значение по умолчанию для бэкенда
    private final String defaultBackend = "213.171.8.149:20001/test-remote";
    // Начальные координаты X и Y
    private float initialX, initialY;
    // Вид джойстика
    private View joystickView;
    // Обработчик для выполнения кода в основном потоке
    private final Handler mainThreadHandler = new Handler();
    // Флаг, указывающий, перетаскивается ли джойстик
    private boolean isJoystickDragging = false;

    /**
     * Этот метод вызывается при создании активности. Здесь происходит инициализация пользовательского интерфейса и установка слушателей.
     *
     * @param savedInstanceState Содержит данные о состоянии активности, если она была ранее уничтожена системой.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);  // Вызов метода суперкласса для создания активности
        setContentView(R.layout.activity_main);  // Установка макета для этой активности

        initializeViews();  // Инициализация элементов пользовательского интерфейса
        setupListeners();  // Установка слушателей для элементов пользовательского интерфейса
    }

    /**
     * Инициализация элементов пользовательского интерфейса.
     * Здесь мы связываем наши переменные с соответствующими элементами макета.
     */
    private void initializeViews() {
        ipInputField = findViewById(R.id.ipInputField);  // Поле ввода текста
        webLogLink = findViewById(R.id.webLogLink);  // Ссылка на лог тестового бэкенда
        backendTestToggle = findViewById(R.id.backendTestToggle);  // Переключатель для тестирования бэкенда
        logDisplayToggle = findViewById(R.id.logDisplayToggle);  // Переключатель для отображения лога
        joystickView = findViewById(R.id.joystickView);  // Джойстик
        logTextField = findViewById(R.id.logTextField);  // Текстовое поле для лога
    }

    /**
     * Установка слушателей для элементов пользовательского интерфейса.
     * Здесь мы устанавливаем слушатели событий для наших элементов пользовательского интерфейса.
     */
    private void setupListeners() {
        // Установка слушателя изменения состояния для переключателя тестирования бэкенда
        backendTestToggle.setOnCheckedChangeListener((buttonView, isChecked) -> toggleBackendTest(isChecked));

        // Установка слушателя изменения состояния для переключателя отображения лога
        logDisplayToggle.setOnCheckedChangeListener((buttonView, isChecked) -> toggleLogVisibility(isChecked));

        // Установка слушателя касания для джойстика
        joystickView.setOnTouchListener((view, event) -> handleJoystickMotion(view, event));

        // Запуск задачи в отдельном потоке с задержкой в 500 миллисекунд
        mainThreadHandler.postDelayed(separateThreadTask(), 500);
    }

    /**
     * Переключение видимости элементов пользовательского интерфейса при включении/выключении тестирования бэкенда.
     *
     * @param isChecked Состояние переключателя тестирования бэкенда.
     */
    private void toggleBackendTest(boolean isChecked) {
        // Если тестирование бэкенда включено, делаем ссылку на лог видимым, иначе - невидимым
        webLogLink.setVisibility(isChecked ? View.VISIBLE : View.INVISIBLE);

        // Если тестирование бэкенда включено, отключаем поле ввода текста и очищаем его, иначе - включаем
        ipInputField.setEnabled(!isChecked);
        ipInputField.setText(isChecked ? defaultBackend : "");

        // Если тестирование бэкенда включено, отключаем переключатель отображения лога и включаем его, иначе - включаем
        logDisplayToggle.setEnabled(!isChecked);
        logDisplayToggle.setChecked(isChecked);
    }

    /**
     * Переключение видимости текста журнала при включении/выключении отображения журнала.
     *
     * @param isChecked Состояние переключателя отображения журнала.
     */
    private void toggleLogVisibility(boolean isChecked) {
        // Если отображение лога включено, делаем текстовое поле для лога видимым, иначе - невидимым
        logTextField.setVisibility(isChecked ? View.VISIBLE : View.INVISIBLE);
    }

    /**
     * Обработка движения джойстика.
     *
     * @param view  Вид джойстика.
     * @param event Событие движения.
     * @return Возвращает true, если событие было обработано, иначе false.
     */
    private boolean handleJoystickMotion(View view, MotionEvent event) {
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                // При нажатии на джойстик начинаем перетаскивание
                startJoystickDrag(view);
                break;

            case MotionEvent.ACTION_MOVE:
                // При движении джойстика, если он перетаскивается, перемещаем его
                if (isJoystickDragging) {
                    moveJoystick(view, event);
                }
                break;

            case MotionEvent.ACTION_UP:
                // При отпускании джойстика останавливаем перетаскивание
                stopJoystickDrag(view);
                break;

            default:
                return false;
        }
        return true;
    }

    /**
     * Создание задачи для выполнения в отдельном потоке.
     * Задача отправляет запрос с координатами джойстика, если он перетаскивается, и повторяется каждые 500 миллисекунд.
     *
     * @return Возвращает задачу в виде объекта Runnable.
     */
    private Runnable separateThreadTask() {
        return new Runnable() {
            @Override
            public void run() {
                // Если джойстик перетаскивается, отправляем запрос с его координатами
                if (isJoystickDragging) {
                    sendRequest(joystickView.getX() - initialX, initialY - joystickView.getY());
                }

                // Повторяем задачу каждые 500 миллисекунд
                mainThreadHandler.postDelayed(this, 500);
            }
        };
    }

    /**
     * Начало перетаскивания джойстика.
     * Здесь мы запускаем обработчик с задержкой и устанавливаем флаг перетаскивания.
     *
     * @param view Вид джойстика.
     */
    private void startJoystickDrag(View view) {
        isJoystickDragging = true;
        initialX = view.getX();
        initialY = view.getY();
    }

    /**
     * Перемещение джойстика.
     * Здесь мы анимируем перемещение джойстика и отправляем запрос с новыми координатами.
     *
     * @param view  Вид джойстика.
     * @param event Событие движения.
     */
    private void moveJoystick(View view, MotionEvent event) {
        // Вычисляем расстояние от начальной точки до точки касания
        float diffX = event.getRawX() - initialX - view.getWidth() / 2;
        float diffY = event.getRawY() - initialY - view.getHeight() / 2 - getStatusBarHeight();
        float distance = (float) Math.sqrt(diffX * diffX + diffY * diffY);

        // Если расстояние превышает максимальное значение, ограничиваем его
        if (distance > 300) {
            diffX = diffX * 300 / distance;
            diffY = diffY * 300 / distance;
        }

        // Обновляем положение джойстика
        joystickView.animate()
                .x(initialX + diffX)
                .y(initialY + diffY)
                .setDuration(0)
                .start();

        sendRequest(view.getX() - initialX, initialY - view.getY());
    }

    /**
     * Остановка перетаскивания джойстика.
     * Здесь мы останавливаем обработчик, возвращаем джойстик в исходное положение анимацией и отправляем запрос с нулевыми координатами.
     *
     * @param view Вид джойстика.
     */
    private void stopJoystickDrag(View view) {
        isJoystickDragging = false;
        joystickView.animate()
                .x(initialX)
                .y(initialY)
                .setDuration(200)
                .start();
        sendRequest(0, 0);  // Отправить нулевые координаты при возвращении в центр
    }

    /**
     * Получение высоты статус-бара (верхняя панель с часами, статусом СИМ-карт и т.д.)
     */
    private int getStatusBarHeight() {
        int result = 0;
        // Получение идентификатора ресурса для высоты статус-бара
        int resourceId = getResources().getIdentifier("status_bar_height", "dimen", "android");
        if (resourceId > 0) {
            // Если идентификатор ресурса существует, получаем размер пикселя для высоты статус-бара
            result = getResources().getDimensionPixelSize(resourceId);
        }
        return result;
    }

    /**
     * Отправка запроса на сервер с координатами x и y.
     */
    private void sendRequest(float x, float y) {
        // Получение IP-адреса из текстового поля
        String ipAddress = ipInputField.getText().toString();
        // Формирование URL для запроса
        String url = "http://" + ipAddress + "/?x=" + Math.round(x) + "&y=" + Math.round(y);
        // Вывод URL в лог
        Log.d("URL", url);

        // Создание очереди запросов
        RequestQueue queue = Volley.newRequestQueue(this);
        // Создание строкового запроса
        StringRequest stringRequest = new StringRequest(Request.Method.GET, url,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        // Обработка ответа сервера
                        Log.d("Response", response);
                        if (logTextField.isEnabled()) {
                            logTextField.append(response + "\n");
                            scrollLog();
                        }
                        Toast.makeText(MainActivity.this, "Запрос успешно отправлен", Toast.LENGTH_SHORT).show();
                    }
                }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                // Обработка ошибки запроса
                Log.e("Error", error.toString());
                if (logTextField.isEnabled()) {
                    String redText = "<font color='red'>" + error + "</font>";
                    logTextField.append(Html.fromHtml(redText, Html.FROM_HTML_MODE_LEGACY) + "\n");
                    scrollLog();
                }
                Toast.makeText(MainActivity.this, "Ошибка при отправке запроса: " + error, Toast.LENGTH_SHORT).show();
            }
        });

        // Добавление запроса в очередь
        queue.add(stringRequest);
    }

    /**
     * Прокрутка текста журнала вниз.
     */
    private void scrollLog() {
        logTextField.post(new Runnable() {
            @Override
            public void run() {
                // Вычисление количества прокрутки
                int scrollAmount = logTextField.getLayout().getLineTop(logTextField.getLineCount()) - logTextField.getHeight();
                // Если требуется прокрутка, прокручиваем вниз, иначе оставляем на месте
                if (scrollAmount > 0)
                    logTextField.scrollTo(0, scrollAmount);
                else
                    logTextField.scrollTo(0, 0);
            }
        });
    }
}
