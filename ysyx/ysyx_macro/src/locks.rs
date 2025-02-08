#[macro_export]
macro_rules! with_mutex_lock {
    ($mutex:expr,$lock:ident, $code:block) => {
        let mut $lock =$mutex.lock();
        $code
        drop($lock);
    };

    ($mutex:expr,$lock:ident, $($mutex_rest:expr, $lock_rest:ident),+,$code:block) => {
        let mut $lock =$mutex.lock();
        with_lock!($($mutex_rest, $lock_rest),+,$code);
        drop($lock);
    };
}

#[macro_export]
macro_rules! with_mutex_lock_expr {
    ($mutex:expr, $lock:ident, $code:block) => {{
        let $lock = $mutex.lock();
        let result = $code;
        result
    }};

    ($mutex:expr,$lock:ident, $($mutex_rest:expr, $lock_rest:ident),+,$code:block) => {
        let $lock =$mutex.lock();
        let result = with_lock_expr!($($mutex_rest, $lock_rest),+,$code);
        result
    };
}

#[macro_export]
macro_rules! with_rwlock_write {
    ($rwlock:expr,$lock:ident, $code:block) => {
        let mut $lock =$rwlock.write();
        $code
        drop($lock);
    };

    ($rwlock:expr,$lock:ident, $($rwlock_rest:expr, $lock_rest:ident),+,$code:block) => {
        let mut $lock =$rwlock.write();
        with_rwlock_write!($($rwlock_rest, $lock_rest),+,$code);
        drop($lock);
    };
}

#[macro_export]
macro_rules! with_rwlock_read {
    ($rwlock:expr,$lock:ident, $code:block) => {{
        let $lock = $rwlock.read();
        let result = $code;
        result
    }};

    ($rwlock:expr,$lock:ident, $($rwlock_rest:expr, $lock_rest:ident),+,$code:block) => {{
        let $lock = $rwlock.read();
        let result = with_rwlock_read_expr!($($rwlock_rest, $lock_rest),+,$code);
        result
    }};
}
