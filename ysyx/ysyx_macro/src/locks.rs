#[macro_export]
macro_rules! with_mutex_lock {
    ($mutex:expr,$lock:ident, $code:block) => {
        let mut $lock =$mutex.lock().unwrap();
        $code
        drop($lock);
    };
    
    ($mutex:expr,$lock:ident, $($mutex_rest:expr, $lock_rest:ident),+,$code:block) => {
        let mut $lock =$mutex.lock().unwrap();
        with_lock!($($mutex_rest, $lock_rest),+,$code);
        drop($lock);
    };
}

#[macro_export]
macro_rules! with_rwlock_write {
    ($rwlock:expr,$lock:ident, $code:block) => {
        let mut $lock =$rwlock.write().unwrap();
        $code
        drop($lock);
    };
    
    ($rwlock:expr,$lock:ident, $($rwlock_rest:expr, $lock_rest:ident),+,$code:block) => {
        let mut $lock =$rwlock.write().unwrap();
        with_rwlock_write!($($rwlock_rest, $lock_rest),+,$code);
        drop($lock);
    };
}

#[macro_export]
macro_rules! with_rwlock_read {
    ($rwlock:expr,$lock:ident, $code:block) => {{
        let $lock = $rwlock.read().unwrap();
        let result = $code;
        result
    }};
    
    ($rwlock:expr,$lock:ident, $($rwlock_rest:expr, $lock_rest:ident),+,$code:block) => {{
        let $lock = $rwlock.read().unwrap();
        let result = with_rwlock_read_expr!($($rwlock_rest, $lock_rest),+,$code);
        result
    }};
}