$(document).ready(function(){
    // update 
    $('#div_userinfo').hide();
    $('#div_signin').hide();

    // logout button
    $('#button_info_logout').on('click', function(){
        logout();
    });

    // login button
    $('#input_password').keyup(function(key){
        if (key.keyCode == 13) login(); // enter key
    });
    $('#button_login').on('click', function(){
        login();
    });

    // sign-up with google button
    $('#btnGoogleLogin').on('click', function(){
        window.location.href = $('#googleLoginUrl').val();
    });

    // sign-up with kakao button
    $('#btnKakaoLogin').on('click', function(){
        window.location.href = $('#kakaoLoginUrl').val();
    });

    // sign-up with naver button
    $('#btnNaverLogin').on('click', function(){
        window.location.href = $('#naverLoginUrl').val();
    });

    // check logged in
    var userJwt = $.cookie('userJwt');
    if (!!userJwt) {
        getUserInfoWihtUpdateUI(userJwt);
    }
    else {
        $('#div_signin').show();
    }
});

function getUserInfoWihtUpdateUI(userJwt) {
    AJAX.apiCall('GET', '/users', {'userJwt':userJwt}, null,
        // Always
        function() {
            // ...
        },
        // Success
        function(apiResult) {
            if (!AJAX.checkResultSuccess(apiResult)) {
                alert(AJAX.getResultMsg(apiResult));
                return;
            }

            $('#input_info_id').val(AJAX.getResultData(apiResult, 'id'));
            $('#input_info_email').val(AJAX.getResultData(apiResult, 'email'));
            $('#input_access_level').val(AJAX.getResultData(apiResult, 'accessLevel'));
            $('#input_info_status').val(AJAX.getResultData(apiResult, 'status'));
            $('#input_info_nickname').val(AJAX.getResultData(apiResult, 'nickname'));
            $('#input_info_full_name').val(AJAX.getResultData(apiResult, 'fullName'));
            $('#input_info_date_of_birth').val(new Date(AJAX.getResultData(apiResult, 'dateOfBirth')).format('yyyy.MM.dd'));
            $('#input_info_gender').val(AJAX.getResultData(apiResult, 'gender'));
            $('#input_info_join_time').val(new Date(AJAX.getResultData(apiResult, 'joinTime')).format('yyyy-MM-dd HH:mm:ss'));
            $('#input_info_last_login_time').val(new Date(AJAX.getResultData(apiResult, 'lastLoginTime')).format('yyyy-MM-dd HH:mm:ss'));
            $('#input_info_service_accessible_time').val(new Date(AJAX.getResultData(apiResult, 'accessibleTime')).format('yyyy-MM-dd HH:mm:ss'));

            // @@ 로그인 시간 유지부분부터 확인. 로그인시간 유지 안시키면 30분이 기본일텐데? 왜 main 다시 호출하면 로그아웃이지?

            $('#div_signin').hide();
            $('#div_userinfo').show();
        },
        // Fail
        function() {
            alert('서버와 통신에 실패했습니다.');
            return;
        },
    )
}

// 로그인 시도
function login() {
    var audience = $('#input_audience').val();
    var email = $('#input_email').val();
    var password = $('#input_password').val();
    var duration = $('input_duration').val();

    if (!audience) {
        alert('페이지에 오류가 발생했습니다.\n(audience: ' + audience + ')');
        return;
    }

    if (!email || email.length == 0) {
        alert('이메일이 비었습니다.');
        return;
    }

    if (!password || password.length == 0) {
        alert('비밀번호가 비었습니다.');
        return;
    }

    if (!duration) {
        duration = 0; // default duration in server
    }

    var reqBody = {
        "audience" : audience,
        "email" : email,
        "password" : SHA256(password + $('#input_clientSalt').val()),
        "duration" : duration
    };

    AJAX.apiCall('POST', '/users/api/jwt/issue', null, reqBody,
        // Always
        function() {
            // alert(JSON.stringify(reqBody));
        },
        // Success
        function(apiResult) {
            if (AJAX.checkResultSuccess(apiResult)) {
                var userJwt = AJAX.getResultData(apiResult, 'userJwt');
                var keepLoggedIn = $('#cb_keep_logged_in').is(':checked');
                afterIssuing(userJwt, keepLoggedIn);
            }
            else {
                alert('로그인에 실패했습니다.\n(' + apiResult.result_message + ')');
            }
        },
        // Fail
        function() {
            alert('서버와 통신에 실패했습니다.');
        }
    );
}

// 로그아웃
function logout() {
    $.removeCookie('userJwt');
    alert('로그아웃 되었습니다.');
    location.replace('/main');
}

// userJwt 발급후 수행
function afterIssuing(userJwt, keepLoggedIn) {
    var afterIssueParam = $('#input_afterIssueParam').val();

    try {
        if (afterIssueParam == 'popup') {
            if (!top.opener) {
                alert('로그인 토큰을 전달받을 부모창을 찾을 수 없습니다.');
                return;
            }

            top.opener.d4r_login_return(userJwt, keepLoggedIn);
            self.close();
        }
        else if (afterIssueParam == 'iframe') {
            if (!parent) {
                alert('로그인 토큰을 전달받을 부모창을 찾을 수 없습니다.');
                return;
            }

            parent.d4r_login_return(userJwt, keepLoggedIn);
        }
        else { // redirection
            var redirectionUrl = null;

            if (!afterIssueParam) {
                redirectionUrl = '/main?userJwt=' + userJwt;
            }
            if (afterIssueParam.indexOf('?') == -1) { // no extra param
                redirectionUrl = afterIssueParam + '?userJwt=' + userJwt + '&keepLoggedIn=' + keepLoggedIn;
            }
            else { // redirect url contains it's own extra param
                redirectionUrl = afterIssueParam + '&userJwt=' + userJwt + '&keepLoggedIn=' + keepLoggedIn;
            }

            location.replace(redirectionUrl);
        }
    }
    catch (e) {
        alert('토큰 전달중 예외가 발생했습니다.\n' + e);
    }
}